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
package org.amdatu.remote.discovery;

import static org.amdatu.remote.ServiceUtil.getServletAlias;

import java.net.URL;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.osgi.service.http.HttpService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;

/**
 * Base class for a Discovery Service that provides HTTP Endpoint discovery through some external form
 * for discovery.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 * 
 * @param <T> the configuration type
 */
public abstract class AbstractHttpEndpointDiscovery<T extends HttpEndpointDiscoveryConfiguration> extends
    AbstractDiscovery {

    private final T m_configuration;
    private volatile ScheduledExecutorService m_executor;
    private volatile HttpService m_http;
    private volatile HttpEndpointDiscoveryServlet m_servlet;
    private volatile HttpEndpointDiscoveryPoller m_poller;

    public AbstractHttpEndpointDiscovery(String name, T configuration) {
        super(name);
        m_configuration = configuration;
    }

    @Override
    protected void startComponent() throws Exception {
        super.startComponent();
        m_executor = Executors.newSingleThreadScheduledExecutor();

        URL localEndpoint = m_configuration.getBaseUrl();

        m_servlet = new HttpEndpointDiscoveryServlet(this);
        m_http.registerServlet(getServletAlias(localEndpoint), m_servlet, null, null);
        m_poller = new HttpEndpointDiscoveryPoller(m_executor, this, m_configuration);
    }

    @Override
    protected void stopComponent() throws Exception {
        try {
            m_http.unregister(getServletAlias(m_configuration.getBaseUrl()));
        }
        catch (Exception e) {
            logWarning("Exception while stopping", e);
        }
        m_servlet = null;

        try {
            m_poller.cancel();
        }
        catch (Exception e) {
            logWarning("Exception while stopping", e);
        }
        m_poller = null;

        try {
            m_executor.shutdown();
            if (!m_executor.awaitTermination(1l, TimeUnit.SECONDS)) {
                m_executor.shutdownNow();
            }
        }
        catch (Exception e) {
            logWarning("Exception while stopping", e);
        }
        m_executor = null;

        super.stopComponent();
    }

    @Override
    protected final void addPublishedEndpoint(EndpointDescription endpoint, String matchedFilter) {
        m_servlet.addEndpoint(endpoint);
    }

    @Override
    protected final void removePublishedEndpoint(EndpointDescription endpoint, String matchedFilter) {
        m_servlet.removeEndpoint(endpoint);

    }

    @Override
    protected final void modifyPublishedEndpoint(EndpointDescription endpoint, String matchedFilter) {
        m_servlet.addEndpoint(endpoint);
    }

    protected final T getConfiguration() {
        return m_configuration;
    }

    protected final void setDiscoveryEndpoints(List<URL> newUrls) {
        m_poller.setDiscoveryEndpoints(newUrls);
    }

    protected final void addDiscoveryEndpoint(URL url) {
        m_poller.addDiscoveryEndpoint(url);
    }

    protected final void removeDiscoveryEndpoint(URL url) {
        m_poller.removeDiscoveryEndpoint(url);
    }
}
