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

import static javax.servlet.http.HttpServletResponse.SC_OK;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.amdatu.remote.AbstractComponentDelegate;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;

/**
 * RSA component that handles all server endpoints.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class HttpServerEndpointHandler extends AbstractComponentDelegate {

    private final Map<String, HttpServerEndpoint> m_handlers = new HashMap<String, HttpServerEndpoint>();
    private final ReentrantReadWriteLock m_lock = new ReentrantReadWriteLock();

    private final RemoteServiceAdminFactory m_factory;

    private final ObjectMapper m_objectMapper = new ObjectMapper();
    private final JsonFactory m_jsonFactory = new JsonFactory(m_objectMapper);
    private static final String APPLICATION_JSON = "application/json";

    public HttpServerEndpointHandler(RemoteServiceAdminFactory factory) {
        super(factory);
        m_factory = factory;
    }

    @Override
    protected void startComponentDelegate() {
        try {
            m_factory.getHttpService().registerServlet(getServletAlias(), new ServerEndpointServlet(), null, null);
        }
        catch (Exception e) {
            logError("Failed to initialize due to configuration problem!", e);
            throw new IllegalStateException("Configuration problem", e);
        }
    }

    @Override
    protected void stopComponentDelegate() {
        m_factory.getHttpService().unregister(getServletAlias());
    }

    /**
     * Returns the runtime URL for a specified Endpoint ID.
     * 
     * @param endpointId The Endpoint ID
     * @return The URL
     * @throws IllegalArgumentException If the Endpoint ID is not a valid URL path segment
     */
    public URL getEndpointURL(String endpointId) {
        try {
            return new URL(m_factory.getBaseURL(), endpointId);
        }
        catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid endpoint id", e);
        }
    }

    /**
     * Add a Server Endpoint.
     * 
     * @param reference The local Service Reference
     * @param endpoint The Endpoint Description
     */
    public HttpServerEndpoint addEndpoint(ServiceReference<?> reference, EndpointDescription endpoint,
        Class<?>[] interfaces) {

        HttpServerEndpoint serverEndpoint = new HttpServerEndpoint(getBundleContext(), reference, interfaces);
        m_lock.writeLock().lock();
        try {
            m_handlers.put(endpoint.getId(), serverEndpoint);
        }
        finally {
            m_lock.writeLock().unlock();
        }
        return serverEndpoint;
    }

    /**
     * Remove a Server Endpoint.
     * 
     * @param endpoint The Endpoint Description
     */
    public HttpServerEndpoint removeEndpoint(EndpointDescription endpoint) {
        HttpServerEndpoint serv;

        m_lock.writeLock().lock();
        try {
            serv = m_handlers.remove(endpoint.getId());
        }
        finally {
            m_lock.writeLock().unlock();
        }
        return serv;
    }

    private HttpServerEndpoint getHandler(String id) {
        m_lock.readLock().lock();
        try {
            return m_handlers.get(id);
        }
        finally {
            m_lock.readLock().unlock();
        }
    }

    private String getServletAlias() {
        String alias = m_factory.getBaseURL().getPath();
        if (!alias.startsWith("/")) {
            alias = "/" + alias;
        }
        if (alias.endsWith("/")) {
            alias = alias.substring(0, alias.length() - 1);
        }
        return alias;
    }

    /**
     * Writes all endpoint ids as a flat JSON array to the given HttpServletResponse
     * 
     * @param req the HttpServletRequest
     * @param resp the HttpServletResponse
     * @throws IOException
     */
    public void listEndpointIds(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        resp.setStatus(SC_OK);
        resp.setContentType(APPLICATION_JSON);

        JsonGenerator gen = m_jsonFactory.createJsonGenerator(resp.getOutputStream());
        gen.writeStartArray();

        m_lock.readLock().lock();
        try {
            for (String endpointId : m_handlers.keySet()) {
                gen.writeString(endpointId);
            }
        }
        finally {
            m_lock.readLock().unlock();
        }

        gen.writeEndArray();
        gen.close();

    }

    /**
     * Internal Servlet that handles all calls.
     */
    private class ServerEndpointServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;

        private final Pattern PATH_PATTERN = Pattern.compile("^\\/{0,1}([A-Za-z0-9-_]+)\\/{0,1}$");

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

            String pathInfo = req.getPathInfo();
            if (pathInfo == null) {
                pathInfo = "";
            }

            Matcher matcher = PATH_PATTERN.matcher(pathInfo);
            if (!matcher.matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid path: " + pathInfo);
                return;
            }
            String endpointId = matcher.group(1);

            HttpServerEndpoint handler = getHandler(endpointId);
            if (handler != null) {
                try {
                    handler.invokeService(req, resp);
                }
                catch (Exception e) {
                    logError("Server Endpoint Handler failed: %s", e, endpointId);
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }
            else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

            // provide endpoint information via http get

            String pathInfo = req.getPathInfo();
            if (pathInfo == null) {
                pathInfo = "";
            }

            // request on root will return an array of endpoint ids
            if (pathInfo.equals("") || pathInfo.equals("/")) {
                listEndpointIds(req, resp);
                return;
            }

            // handle requested endpoint
            Matcher matcher = PATH_PATTERN.matcher(pathInfo);
            if (!matcher.matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid path: " + pathInfo);
                return;
            }

            String endpointId = matcher.group(1);

            HttpServerEndpoint handler = getHandler(endpointId);
            if (handler != null) {
                try {
                    handler.listMethodSignatures(req, resp);
                }
                catch (Exception e) {
                    logError("Server Endpoint Handler failed: %s", e, endpointId);
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }
            else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }
}
