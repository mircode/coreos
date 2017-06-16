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

import static org.amdatu.remote.EndpointUtil.computeHash;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.EXPORT_ERROR;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.EXPORT_WARNING;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;

/**
 * The {@link ExportedEndpointImpl} class represents an active exported endpoint for a
 * unique {@link EndpointDescription}. It manages the server endpoint lifecycle and
 * serves as the {@link ExportRegistration} and {@link ExportReference}.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class ExportedEndpointImpl implements ExportRegistration, ExportReference, ServerEndpointProblemListener {

    private final AtomicBoolean m_closed = new AtomicBoolean(false);
    private final RemoteServiceAdminImpl m_admin;
    private final ServiceReference<?> m_reference;

    private volatile EndpointDescription m_endpoint;
    private volatile String m_endpointHash;
    private volatile Throwable m_exception;
    private volatile Map<String, ?> m_properties; // original export properties
    private volatile HttpServerEndpoint m_serverEndpoint;

    /**
     * Constructs an {@link ExportRegistrationImpl} and registers the server endpoint. Any input validation
     * should have been done. Exceptions that occur during construction or registration result in an invalid
     * export registration and are therefore accessible through {@link #getException()}.
     * 
     * @param admin the admin instance
     * @param description the description
     * @param reference the service reference
     * @param properties the export properties
     */
    public ExportedEndpointImpl(RemoteServiceAdminImpl admin, EndpointDescription description,
        ServiceReference<?> reference, Map<String, ?> properties, HttpServerEndpoint serverEndpoint) {

        m_admin = admin;
        m_endpoint = description;
        m_endpointHash = computeHash(description);
        m_reference = reference;
        m_serverEndpoint = serverEndpoint;
        m_serverEndpoint.setProblemListener(this);
    }

    @Override
    public void handleEndpointError(Throwable exception) {
        m_admin.getEventsHandler().emitEvent(EXPORT_ERROR, m_admin.getBundleContext().getBundle(), this, exception);
    }

    @Override
    public void handleEndpointWarning(Throwable exception) {
        m_admin.getEventsHandler().emitEvent(EXPORT_WARNING, m_admin.getBundleContext().getBundle(), this, exception);
    }

    @Override
    public ExportReference getExportReference() {
        if (m_closed.get()) {
            return null;
        }
        if (m_exception != null) {
            throw new IllegalStateException("Endpoint registration is failed. See #getException()");
        }
        return this;
    }

    @Override
    public EndpointDescription update(Map<String, ?> properties) {
        if (m_closed.get()) {
            throw new IllegalStateException("Updating closed Export Registration not supported");
        }
        if (m_exception != null) {
            throw new IllegalStateException("Updating invalid Export Registration not allowed");
        }
        if (properties != null) {
            m_properties = properties;
        }

        EndpointDescription updateDescription =
            m_admin.createEndpointDescription(m_endpoint.getId(), m_reference, m_properties);
        if (updateDescription == null) {
            // TODO set exception?
            return null;
        }

        String updateHash = computeHash(updateDescription);
        if (!updateDescription.equals(m_endpointHash)) {
            m_endpoint = updateDescription;
            m_endpointHash = updateHash;
            // TODO m_endpoint#update()
            m_admin.exportedEndpointUpdated(this);
        }

        return m_endpoint;
    }

    @Override
    public void close() {
        if (!m_closed.compareAndSet(false, true)) {
            return;
        }
        if (m_serverEndpoint != null) {
            m_admin.getServerEndpointHandler().removeEndpoint(m_endpoint);
            m_serverEndpoint = null;
        }
        m_admin.exportedEndpointClosed(this);
    }

    @Override
    public Throwable getException() {
        return getException(false);
    }

    @Override
    public ServiceReference<?> getExportedService() {
        return getExportedService(false);
    }

    @Override
    public EndpointDescription getExportedEndpoint() {
        return getExportedEndpoint(false);
    }

    EndpointDescription getExportedEndpoint(boolean ignoreClosed) {
        if (!ignoreClosed && m_closed.get()) {
            return null;
        }
        return m_endpoint;
    }

    ServiceReference<?> getExportedService(boolean ignoreClosed) {
        if (!ignoreClosed && m_closed.get()) {
            return null;
        }
        return m_reference;
    }

    Throwable getException(boolean ignoreClosed) {
        if (!ignoreClosed && m_closed.get()) {
            return null;
        }
        return m_exception;
    }
}
