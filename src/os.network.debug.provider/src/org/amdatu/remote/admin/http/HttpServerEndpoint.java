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

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static org.amdatu.remote.admin.http.HttpAdminUtil.getMethodSignature;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.amdatu.remote.IOUtil;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.type.JavaType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Servlet that represents a remoted local service.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class HttpServerEndpoint {

    private static final int FATAL_ERROR_COUNT = 5;

    private static final String APPLICATION_JSON = "application/json";

    private final ObjectMapper m_objectMapper = new ObjectMapper();
    private final JsonFactory m_jsonFactory = new JsonFactory(m_objectMapper);

    private final BundleContext m_bundleContext;
    private final ServiceReference<?> m_serviceReference;
    private final Map<String, Method> m_interfaceMethods;

    private ServerEndpointProblemListener m_problemListener;
    private int m_localErrors;

    public HttpServerEndpoint(BundleContext context, ServiceReference<?> reference, Class<?>... interfaceClasses) {

        m_bundleContext = context;
        m_serviceReference = reference;
        m_interfaceMethods = new HashMap<String, Method>();

        for (Class<?> interfaceClass : interfaceClasses) {
            for (Method method : interfaceClass.getMethods()) {
                // Although we're accessing a public (interface) method, the *service* implementation
                // itself can be non-public. This check appears to be fixed in recent Java versions...
                method.setAccessible(true);
                m_interfaceMethods.put(getMethodSignature(method), method);
            }
        }
    }

    /**
     * @param problemListener the problem listener to set, can be <code>null</code>.
     */
    public void setProblemListener(ServerEndpointProblemListener problemListener) {
        m_problemListener = problemListener;
    }

    public void invokeService(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        InputStream in = req.getInputStream();
        Object service = null;
        try {

            JsonNode tree = m_objectMapper.readTree(in);
            if (tree == null) {
                resp.sendError(SC_BAD_REQUEST);
                return;
            }

            JsonNode signatureNode = tree.get("m");
            if (signatureNode == null) {
                resp.sendError(SC_BAD_REQUEST);
                return;
            }

            JsonNode argumentsNode = tree.get("a");
            if (argumentsNode == null) {
                resp.sendError(SC_BAD_REQUEST);
                return;
            }

            ArrayNode arguments = m_objectMapper.readValue(argumentsNode, ArrayNode.class);
            if (arguments == null) {
                resp.sendError(SC_BAD_REQUEST);
                return;
            }

            Method method = m_interfaceMethods.get(signatureNode.asText());
            if (method == null) {
                resp.sendError(SC_NOT_FOUND);
                return;
            }

            Type[] types = method.getGenericParameterTypes();
            if (arguments.size() != types.length) {
                resp.sendError(SC_BAD_REQUEST);
                return;
            }

            Object[] parameters = new Object[types.length];
            for (int i = 0; i < types.length; i++) {
                try {
                    JavaType argType =
                        m_objectMapper.getTypeFactory().constructType(types[i]);
                    parameters[i] = m_objectMapper.readValue(arguments.get(i), argType);
                }
                catch (Exception e) {
                    resp.sendError(SC_BAD_REQUEST);
                    return;
                }
            }

            service = m_bundleContext.getService(m_serviceReference);
            if (service == null) {
                handleLocalException(null);
                resp.sendError(SC_SERVICE_UNAVAILABLE);
                return;
            }

            Object result = null;
            Exception exception = null;
            try {
                result = method.invoke(service, parameters);
            }
            catch (Exception e) {
                exception = e;
            }

            resp.setStatus(SC_OK);
            resp.setContentType(APPLICATION_JSON);

            JsonGenerator gen = m_jsonFactory.createJsonGenerator(resp.getOutputStream());
            gen.writeStartObject();
            if (exception != null) {
                gen.writeObjectField("e", new ExceptionWrapper(unwrapException(exception)));
            }
            else if (!Void.TYPE.equals(method.getReturnType())) {
                gen.writeObjectField("r", result);
            }
            gen.close();

            // All is fine.. reset the local error count
            m_localErrors = 0;
        }
        finally {
            IOUtil.closeSilently(in);
            if (service != null) {
                service = null;
                try {
                    m_bundleContext.ungetService(m_serviceReference);
                }
                catch (Exception e) {
                    // ignore... we at least tried
                }
            }
        }
    }

    /**
     * Handles I/O exceptions by counting the number of times they occurred, and if a certain
     * threshold is exceeded closes the import registration for this endpoint.
     * 
     * @param e the exception to handle.
     */
    private void handleLocalException(IOException e) {
        if (m_problemListener != null) {
            if (++m_localErrors > FATAL_ERROR_COUNT) {
                m_problemListener.handleEndpointError(e);
            }
            else {
                m_problemListener.handleEndpointWarning(e);
            }
        }
    }

    /**
     * Unwraps a given {@link Exception} into a more concrete exception if it represents an {@link InvocationTargetException}.
     * 
     * @param e the exception to unwrap, should not be <code>null</code>.
     * @return the (unwrapped) throwable or exception, never <code>null</code>.
     */
    private static Throwable unwrapException(Exception e) {
        if (e instanceof InvocationTargetException) {
            return ((InvocationTargetException) e).getTargetException();
        }
        return e;
    }

    /**
     * Writes all method signatures as a flat JSON array to the given HttpServletResponse
     * 
     * @param req the HttpServletRequest
     * @param resp the HttpServletResponse
     * @throws IOException
     */
    public void listMethodSignatures(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        resp.setStatus(SC_OK);
        resp.setContentType(APPLICATION_JSON);

        JsonGenerator gen = m_jsonFactory.createJsonGenerator(resp.getOutputStream());
        gen.writeStartArray();

        for (String signature : m_interfaceMethods.keySet()) {
            gen.writeString(signature);
        }

        gen.writeEndArray();
        gen.close();

    }
}
