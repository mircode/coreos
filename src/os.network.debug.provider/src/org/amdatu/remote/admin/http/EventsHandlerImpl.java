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

import java.security.Permission;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.amdatu.remote.AbstractComponentDelegate;
import org.amdatu.remote.ServiceUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.TopicPermission;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointPermission;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;

/**
 * RSA component that handles events delivery.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class EventsHandlerImpl extends AbstractComponentDelegate {

    private static final String EVENT_TOPIC_BASE = "org/osgi/service/remoteserviceadmin/";

    private static final Permission EVENT_TOPIC_PERMISSION = new TopicPermission(
        "org/osgi/service/remoteserviceadmin/*", TopicPermission.PUBLISH);

    private final Map<ServiceReference<?>, RemoteServiceAdminListener> m_listeners =
        new ConcurrentHashMap<ServiceReference<?>, RemoteServiceAdminListener>();

    private final Map<ServiceReference<?>, EventAdmin> m_admins =
        new ConcurrentHashMap<ServiceReference<?>, EventAdmin>();

    public EventsHandlerImpl(RemoteServiceAdminFactory adminManager) {
        super(adminManager);
    }

    // Dependency Manager callback method
    protected final void listenerAdded(ServiceReference<?> reference, RemoteServiceAdminListener listener) {
        logDebug("RSA listener added %s - %s", reference, listener);
        m_listeners.put(reference, listener);
    }

    // Dependency Manager callback method
    protected final void listenerRemoved(ServiceReference<?> reference, RemoteServiceAdminListener listener) {
        logDebug("RSA listener removed %s - %s", reference, listener);
        m_listeners.remove(reference);
    }

    // Dependency Manager callback method
    protected final void eventAdminAdded(ServiceReference<?> reference, EventAdmin eventAdmin) {
        logDebug("EventAdmin added %s", reference);
        m_admins.put(reference, eventAdmin);
    }

    // Dependency Manager callback method
    protected final void eventAdminRemoved(ServiceReference<?> reference, EventAdmin eventAdmin) {
        logDebug("EventAdmin removed %s", reference);
        m_admins.remove(reference);
    }

    /*
     * 'API' methods
     */

    public void emitEvent(int type, Bundle source, ExportReference exportRef, Throwable exception) {
        RemoteServiceAdminEvent event = new RemoteServiceAdminEvent(type, source, exportRef, exception);

        // FIXME why are we casting here?
        EndpointDescription desc = null;
        if (exportRef instanceof ExportedEndpointImpl) {
            desc = ((ExportedEndpointImpl) exportRef).getExportedEndpoint(true);
        }
        else {
            desc = exportRef.getExportedEndpoint();
        }

        if (!m_listeners.isEmpty()) {
            emitRemoteServiceAdminEvent(event, desc);
        }
        else {
            logDebug("No RSA listeners");
        }
        if (!m_admins.isEmpty()) {
            emitEventAdminEvent(createEventAdminEvent(event, desc, exception));
        }
        else {
            logDebug("No EventAdmins");
        }
    }

    public void emitEvent(int type, Bundle source, ImportReference importRef, Throwable exception) {
        RemoteServiceAdminEvent event = new RemoteServiceAdminEvent(type, source, importRef, exception);

        // FIXME why are we casting here?
        EndpointDescription desc = null;
        if (importRef instanceof ImportedEndpointImpl) {
            desc = ((ImportedEndpointImpl) importRef).getImportedEndpoint(true);
        }
        else {
            desc = importRef.getImportedEndpoint();
        }

        if (!m_listeners.isEmpty()) {
            emitRemoteServiceAdminEvent(event, desc);
        }
        if (!m_admins.isEmpty()) {
            emitEventAdminEvent(createEventAdminEvent(event, desc, exception));
        }
    }

    /*
     * Private methods
     */

    private void emitRemoteServiceAdminEvent(RemoteServiceAdminEvent event, EndpointDescription endpoint) {
        EndpointPermission permission =
            new EndpointPermission(endpoint, ServiceUtil.getFrameworkUUID(getBundleContext()), EndpointPermission.READ);

        for (Entry<ServiceReference<?>, RemoteServiceAdminListener> entry : m_listeners.entrySet()) {
            if (entry.getKey().getBundle().hasPermission(permission)) {
                logDebug("Calling RSA listener %s - %s", entry.getKey(), entry.getValue());
                entry.getValue().remoteAdminEvent(event);
            }
        }
    }

    private void emitEventAdminEvent(final Event event) {
        try {
            // AMDATURS-84: make sure we only check permissions when there's a SecurityManager present...
            SecurityManager securityManager = System.getSecurityManager();
            if (securityManager != null) {
                securityManager.checkPermission(EVENT_TOPIC_PERMISSION);
            }

            for (EventAdmin eventAdmin : m_admins.values()) {
                eventAdmin.postEvent(event);
            }
        }
        catch (Exception e) {
            logWarning("No permission to post events!", e);
        }
    }

    /**
     * Map a Remote Service Admin Event to an EventAdmin event according to OSGi Enterprise R5 122.7.1.
     * 
     * @param event the Remote Service Admin event
     * @return the Event Admin event
     */
    private Event createEventAdminEvent(RemoteServiceAdminEvent event, EndpointDescription description,
        Throwable exception) {

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("bundle", getBundleContext().getBundle());
        properties.put("bundle.id", getBundleContext().getBundle().getBundleId());
        properties.put("bundle.symbolicname", getBundleContext().getBundle().getSymbolicName());
        properties.put("bundle.version", getBundleContext().getBundle().getVersion());
        properties.put("bundle.signer", new String[] {}); // TODO impl
        properties.put("event", event);
        properties.put("timestamp", System.currentTimeMillis());
        putIfValueNotNull(properties, "cause", exception);

        if (description != null) {
            putIfValueNotNull(properties, "endpoint.service.id", description.getServiceId());
            putIfValueNotNull(properties, "endpoint.framework.uuid", description.getFrameworkUUID());
            putIfValueNotNull(properties, "endpoint.id", description.getId());
            putIfValueNotNull(properties, "endpoint.imported.configs", description.getConfigurationTypes());
        }
        return new Event(getEventTopic(event.getType()), properties);
    }

    private static String getEventTopic(int type) {
        return EVENT_TOPIC_BASE + getEventName(type);
    }

    private static String getEventName(int type) {
        switch (type) {
            case RemoteServiceAdminEvent.EXPORT_REGISTRATION:
                return "EXPORT_REGISTRATION";
            case RemoteServiceAdminEvent.EXPORT_UNREGISTRATION:
                return "EXPORT_UNREGISTRATION";
            case RemoteServiceAdminEvent.EXPORT_UPDATE:
                return "EXPORT_UPDATE";
            case RemoteServiceAdminEvent.EXPORT_WARNING:
                return "EXPORT_WARNING";
            case RemoteServiceAdminEvent.EXPORT_ERROR:
                return "EXPORT_ERROR";
            case RemoteServiceAdminEvent.IMPORT_REGISTRATION:
                return "IMPORT_REGISTRATION";
            case RemoteServiceAdminEvent.IMPORT_UNREGISTRATION:
                return "IMPORT_UNREGISTRATION";
            case RemoteServiceAdminEvent.IMPORT_UPDATE:
                return "IMPORT_UPDATE";
            case RemoteServiceAdminEvent.IMPORT_WARNING:
                return "IMPORT_WARNING";
            case RemoteServiceAdminEvent.IMPORT_ERROR:
                return "IMPORT_ERROR";
            default:
                throw new IllegalStateException("Unknown event type : " + type);
        }
    }

    private static void putIfValueNotNull(Map<String, Object> properties, String key, Object value) {
        if (value != null) {
            properties.put(key, value);
        }
    }
}
