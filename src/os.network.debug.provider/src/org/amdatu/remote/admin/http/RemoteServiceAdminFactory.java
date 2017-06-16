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

import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.amdatu.remote.AbstractComponent;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

/**
 * Factory for the Amdatu Remote Service Admin service implementation.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class RemoteServiceAdminFactory extends AbstractComponent implements ServiceFactory<RemoteServiceAdmin> {

    private final ConcurrentHashMap<Bundle, RemoteServiceAdminImpl> m_instances =
        new ConcurrentHashMap<Bundle, RemoteServiceAdminImpl>();

    private final HttpAdminConfiguration m_configuration;
    private final EventsHandlerImpl m_eventsHandler;
    private final HttpServerEndpointHandler m_endpointHandler;

    private volatile HttpService m_httpService;

    public RemoteServiceAdminFactory(HttpAdminConfiguration configuration) {
        super("admin", "http");
        m_configuration = configuration;
        m_eventsHandler = new EventsHandlerImpl(this);
        m_endpointHandler = new HttpServerEndpointHandler(this);
    }

    @Override
    protected void startComponent() throws Exception {
        m_eventsHandler.start();
        m_endpointHandler.start();
    }

    @Override
    protected void stopComponent() throws Exception {
        m_eventsHandler.stop();
        m_endpointHandler.stop();
    }

    @Override
    public RemoteServiceAdmin getService(Bundle bundle, ServiceRegistration<RemoteServiceAdmin> registration) {

        RemoteServiceAdminImpl instance = new RemoteServiceAdminImpl(this, m_configuration);
        try {
            instance.start();
            RemoteServiceAdminImpl previous = m_instances.put(bundle, instance);
            assert previous == null; // framework should guard against this
            return instance;
        }
        catch (Exception e) {
            logError("Exception while instantiating admin instance!", e);
            return null;
        }
    }

    @Override
    public void ungetService(Bundle bundle, ServiceRegistration<RemoteServiceAdmin> registration,
        RemoteServiceAdmin service) {

        RemoteServiceAdminImpl instance = m_instances.remove(bundle);
        try {
            instance.stop();
        }
        catch (Exception e) {}
    }

    /*
     * Internal access
     */

    Collection<ImportReference> getAllImportedEndpoints() {
        Set<ImportReference> importedEndpoints = new HashSet<ImportReference>();
        for (RemoteServiceAdminImpl admin : m_instances.values()) {
            admin.addImportedEndpoints(importedEndpoints);
        }
        return importedEndpoints;
    }

    Collection<ExportReference> getAllExportedEndpoints() {
        Set<ExportReference> exportedEndpoints = new HashSet<ExportReference>();
        for (RemoteServiceAdminImpl admin : m_instances.values()) {
            admin.addExportedEndpoints(exportedEndpoints);
        }
        return exportedEndpoints;
    }

    HttpService getHttpService() {
        return m_httpService;
    }

    EventsHandlerImpl getEventsHandler() {
        return m_eventsHandler;
    }

    HttpServerEndpointHandler getServerEndpointHandler() {
        return m_endpointHandler;
    }

    URL getBaseURL() {
        return m_configuration.getBaseUrl();
    }
}
