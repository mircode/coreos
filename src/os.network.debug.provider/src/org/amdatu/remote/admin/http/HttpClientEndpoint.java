/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.amdatu.remote.admin.http;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.amdatu.remote.IOUtil.closeSilently;
import static org.amdatu.remote.admin.http.HttpAdminUtil.getMethodSignature;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.JavaType;
import org.osgi.framework.ServiceException;

/**
 * Implementation of an {@link InvocationHandler} that represents a remoted service for one or more service interfaces.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class HttpClientEndpoint implements InvocationHandler {

    private static final int FATAL_ERROR_COUNT = 5;

    private final ObjectMapper m_objectMapper = new ObjectMapper();
    private final JsonFactory m_JsonFactory = new JsonFactory(m_objectMapper);

    private final Map<Method, String> m_interfaceMethods;
    private final URL m_serviceURL;
    private final Object m_proxy;
    private final HttpAdminConfiguration m_configuration;

    private ClientEndpointProblemListener m_problemListener;
    private int m_remoteErrors;

    public HttpClientEndpoint(URL serviceURL, HttpAdminConfiguration configuration, Class<?>... interfaceClasses) {
        if (interfaceClasses.length == 0) {
            throw new IllegalArgumentException("Need at least one interface to expose!");
        }
        m_interfaceMethods = new HashMap<Method, String>();
        m_serviceURL = serviceURL;
        m_proxy = Proxy.newProxyInstance(getClass().getClassLoader(), interfaceClasses, this);
        m_configuration = configuration;
        m_remoteErrors = 0;

        for (Class<?> interfaceClass : interfaceClasses) {
            for (Method method : interfaceClass.getMethods()) {
                m_interfaceMethods.put(method, getMethodSignature(method));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public final <T> T getServiceProxy() {
        return (T) m_proxy;
    }

    @Override
    public final Object invoke(Object serviceProxy, Method method, Object[] args) throws Throwable {
        if (m_interfaceMethods.containsKey(method)) {
            return invokeRemoteMethod(method, args);
        }
        return method.invoke(m_serviceURL, args);
    }

    /**
     * @param problemListener the problem listener to set, can be <code>null</code>.
     */
    public void setProblemListener(ClientEndpointProblemListener problemListener) {
        m_problemListener = problemListener;
    }

    /**
     * Handles I/O exceptions by counting the number of times they occurred, and if a certain
     * threshold is exceeded closes the import registration for this endpoint.
     * 
     * @param e the exception to handle.
     */
    private void handleRemoteException(IOException e) {
        if (m_problemListener != null) {
            if (++m_remoteErrors > FATAL_ERROR_COUNT) {
                m_problemListener.handleEndpointError(e);
            }
            else {
                m_problemListener.handleEndpointWarning(e);
            }
        }
    }

    /**
     * Does the invocation of the remote method adhering to any security managers that might be installed.
     * 
     * @param method the actual method to invoke;
     * @param arguments the arguments of the method to invoke;
     * @return the result of the method invocation, can be <code>null</code>.
     * @throws Exception in case the invocation failed in some way.
     */
    private Object invokeRemoteMethod(final Method method, final Object[] arguments) throws Throwable {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            try {
                return AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    @Override
                    public Object run() {
                        try {
                            return invokeRemoteMethodSecure(method, arguments);
                        }
                        catch (Throwable e) {
                            throw new ServiceException("TRANSPORT WRAPPER", e);
                        }
                    }
                });
            }
            catch (ServiceException e) {
                // All exceptions are wrapped in this exception, so we need to rethrow its cause to get the actual exception back...
                throw e.getCause();
            }
        }
        else {
            return invokeRemoteMethodSecure(method, arguments);
        }
    }

    /**
     * Does the actual invocation of the remote method.
     * <p>
     * This method assumes that all security checks (if needed) are processed!
     * </p>
     * 
     * @param method the actual method to invoke;
     * @param arguments the arguments of the method to invoke;
     * @return the result of the method invocation, can be <code>null</code>.
     * @throws Exception in case the invocation failed in some way.
     */
    private Object invokeRemoteMethodSecure(Method method, Object[] arguments) throws Throwable {

        HttpURLConnection connection = null;
        OutputStream outputStream = null;
        InputStream inputStream = null;
        Object result = null;
        ExceptionWrapper exception = null;
        try {
            connection = (HttpURLConnection) m_serviceURL.openConnection();
            connection.setRequestMethod("POST");
            connection.setUseCaches(false);
            connection.setDoOutput(true);
            connection.setConnectTimeout(m_configuration.getConnectTimeout());
            connection.setReadTimeout(m_configuration.getReadTimeout());
            connection.setRequestProperty("Content-Type", "application/json");
            connection.connect();
            outputStream = connection.getOutputStream();
            writeMethodInvocationJSON(outputStream, method, arguments);

            int rc = connection.getResponseCode();
            switch (rc) {
                case HTTP_OK:
                    inputStream = connection.getInputStream();
                    JsonNode tree = m_objectMapper.readTree(inputStream);
                    if (tree != null) {
                        JsonNode exceptionNode = tree.get("e");
                        if (exceptionNode != null) {
                            exception = m_objectMapper.readValue(exceptionNode, ExceptionWrapper.class);
                        }
                        else {
                            JsonNode responseNode = tree.get("r");
                            if (responseNode != null) {
                                JavaType returnType =
                                    m_objectMapper.getTypeFactory().constructType(method.getGenericReturnType());
                                result = m_objectMapper.readValue(responseNode, returnType);
                            }
                        }
                    }
                    break;
                default:
                    throw new IOException("Unexpected HTTP response: " + rc + " " + connection.getResponseMessage());
            }
            // Reset this error counter upon each successful request...
            m_remoteErrors = 0;
        }
        catch (IOException e) {
            handleRemoteException(e);
            throw new ServiceException("Remote service invocation failed: " + e.getMessage(), ServiceException.REMOTE,
                e);
        }
        finally {
            closeSilently(inputStream, outputStream);
            if (connection != null) {
                connection.disconnect();
            }
        }

        if (exception != null) {
            throw exception.getException();
        }
        return result;
    }

    /**
     * Writes out the the invocation payload as a JSON object with with two fields. The m-field holds the method's signature
     * and the a-field hold the arguments array.
     * 
     * @param out the output stream to write to
     * @param method the method in question
     * @param arguments the arguments
     * @throws IOException if a write operation fails
     */
    private void writeMethodInvocationJSON(OutputStream out, Method method, Object[] arguments) throws IOException {
        JsonGenerator gen = m_JsonFactory.createJsonGenerator(out);
        gen.writeStartObject();
        gen.writeStringField("m", m_interfaceMethods.get(method));
        gen.writeArrayFieldStart("a");
        if (arguments != null) {
            for (int i = 0; i < arguments.length; i++) {
                gen.writeObject(arguments[i]);
            }
        }
        gen.writeEndArray();
        gen.flush();
        gen.close();
    }
}
