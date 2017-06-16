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

import static org.amdatu.remote.ServiceUtil.getStringPlusValue;
import static org.amdatu.remote.admin.http.HttpAdminConstants.CONFIGURATION_TYPE;
import static org.amdatu.remote.admin.http.HttpAdminConstants.ENDPOINT_URL;
import static org.amdatu.remote.admin.http.HttpAdminConstants.PASSBYVALYE_INTENT;
import static org.amdatu.remote.admin.http.HttpAdminConstants.SUPPORTED_INTENTS;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.framework.Constants.SERVICE_ID;
import static org.osgi.service.remoteserviceadmin.EndpointPermission.EXPORT;
import static org.osgi.service.remoteserviceadmin.EndpointPermission.IMPORT;
import static org.osgi.service.remoteserviceadmin.EndpointPermission.READ;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_FRAMEWORK_UUID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_ID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_SERVICE_ID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_CONFIGS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_INTENTS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_INTENTS_EXTRA;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_INTERFACES;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_IMPORTED_CONFIGS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_INTENTS;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.EXPORT_REGISTRATION;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.EXPORT_UNREGISTRATION;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.EXPORT_UPDATE;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.IMPORT_REGISTRATION;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.IMPORT_UNREGISTRATION;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.IMPORT_UPDATE;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.amdatu.remote.AbstractComponentDelegate;
import org.amdatu.remote.ServiceUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointPermission;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

/**
 * Remote Service Admin instance implementation.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class RemoteServiceAdminImpl extends AbstractComponentDelegate implements RemoteServiceAdmin {

    private static final Set<String> MY_SUPPORTED_INTENTS_SET = new HashSet<String>(
        Arrays.asList(SUPPORTED_INTENTS));

    private final Map<EndpointDescription, Set<ExportedEndpointImpl>> m_exportedEndpoints =
        new HashMap<EndpointDescription, Set<ExportedEndpointImpl>>();

    private final Map<EndpointDescription, Set<ImportedEndpointImpl>> m_importedEndpoints =
        new HashMap<EndpointDescription, Set<ImportedEndpointImpl>>();

    private final RemoteServiceAdminFactory m_manager;
    private final HttpAdminConfiguration m_configuration;

    public RemoteServiceAdminImpl(RemoteServiceAdminFactory manager, HttpAdminConfiguration configuration) {
        super(manager);
        m_manager = manager;
        m_configuration = configuration;
    }

    @Override
    protected void startComponentDelegate() throws Exception {
    }

    @Override
    protected void stopComponentDelegate() throws Exception {

        synchronized (m_exportedEndpoints) {
            for (Set<ExportedEndpointImpl> exportedEndpoints : m_exportedEndpoints.values()) {
                for (ExportedEndpointImpl exportedEndpoint : exportedEndpoints) {
                    exportedEndpoint.close();
                }
            }
        }

        synchronized (m_importedEndpoints) {
            for (Set<ImportedEndpointImpl> importedEndpoints : m_importedEndpoints.values()) {
                for (ImportedEndpointImpl importedEndpoint : importedEndpoints) {
                    importedEndpoint.close();
                }
            }
        }
    }

    @Override
    public Collection<ExportRegistration> exportService(final ServiceReference<?> reference,
        final Map<String, ?> properties) {

        final EndpointDescription endpoint = createEndpointDescription(reference, properties);
        if (endpoint == null) {
            return Collections.emptyList();
        }

        final Class<?>[] interfaces = loadEndpointInterfaces(reference, endpoint);
        if (interfaces == null) {
            return Collections.emptyList();
        }

        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(new EndpointPermission(endpoint, ServiceUtil.getFrameworkUUID(getBundleContext()),
                EXPORT));
        }

        return AccessController.doPrivileged(new PrivilegedAction<Collection<ExportRegistration>>() {

            @Override
            public Collection<ExportRegistration> run() {

                ExportedEndpointImpl exportedEndpoint = null;
                synchronized (m_exportedEndpoints) {
                    Set<ExportedEndpointImpl> exportedEndpoints = m_exportedEndpoints.get(endpoint);
                    if (exportedEndpoints == null) {
                        exportedEndpoints = new HashSet<ExportedEndpointImpl>();
                        m_exportedEndpoints.put(endpoint, exportedEndpoints);
                    }

                    HttpServerEndpoint serverEndpoint =
                        getServerEndpointHandler().addEndpoint(reference, endpoint, interfaces);
                    exportedEndpoint =
                        new ExportedEndpointImpl(RemoteServiceAdminImpl.this, endpoint, reference, properties,
                            serverEndpoint);
                    exportedEndpoints.add(exportedEndpoint);
                    logDebug("Added exported endpoint: %s", exportedEndpoint);
                }
                m_manager.getEventsHandler().emitEvent(EXPORT_REGISTRATION, getBundleContext().getBundle(),
                    exportedEndpoint, exportedEndpoint.getException());
                return Collections.singletonList((ExportRegistration) exportedEndpoint);
            }
        });
    }

    @Override
    public ImportRegistration importService(final EndpointDescription endpoint) {

        if (endpoint == null) {
            logWarning("No valid endpoint specified. Ignoring...");
            return null;
        }

        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(new EndpointPermission(endpoint,
                ServiceUtil.getFrameworkUUID(getBundleContext()), IMPORT));
        }

        if (!endpoint.getConfigurationTypes().contains(CONFIGURATION_TYPE)) {
            logInfo("No supported configuration type found. Not importing endpoint: %s", endpoint);
            return null;
        }

        if (!hasAvailableInterfaces(endpoint)) {
            logInfo("No available interfaces found. Not importing endpoint: %s", endpoint);
            return null;
        }

        if (!hasValidServiceLocation(endpoint)) {
            logInfo("No valid service location found. Not importing endpoint: %s", endpoint);
            return null;
        }

        return AccessController.doPrivileged(new PrivilegedAction<ImportRegistration>() {

            @Override
            public ImportRegistration run() {

                ImportedEndpointImpl importedEndpoint = null;
                synchronized (m_importedEndpoints) {
                    Set<ImportedEndpointImpl> importedEndpoints = m_importedEndpoints.get(endpoint);
                    if (importedEndpoints == null) {
                        importedEndpoints = new HashSet<ImportedEndpointImpl>();
                        m_importedEndpoints.put(endpoint, importedEndpoints);
                    }
                    importedEndpoint = new ImportedEndpointImpl(RemoteServiceAdminImpl.this, endpoint, m_configuration);
                    importedEndpoints.add(importedEndpoint);
                    logDebug("Added imported endpoint: %s", importedEndpoint);
                }
                getEventsHandler().emitEvent(IMPORT_REGISTRATION, getBundleContext().getBundle(), importedEndpoint,
                    importedEndpoint.getException());
                return importedEndpoint;
            }
        });
    }

    @Override
    public Collection<ImportReference> getImportedEndpoints() {

        return Collections.unmodifiableCollection(m_manager.getAllImportedEndpoints());
    }

    @Override
    public Collection<ExportReference> getExportedServices() {

        return Collections.unmodifiableCollection(m_manager.getAllExportedEndpoints());
    }

    void addExportedEndpoints(Collection<ExportReference> collection) {

        SecurityManager securityManager = System.getSecurityManager();
        String frameworkUUID = ServiceUtil.getFrameworkUUID(getBundleContext());
        synchronized (m_exportedEndpoints) {
            for (Entry<EndpointDescription, Set<ExportedEndpointImpl>> entry : m_exportedEndpoints.entrySet()) {
                try {
                    if (securityManager != null) {
                        securityManager.checkPermission(new EndpointPermission(entry.getKey(), frameworkUUID, READ));
                    }
                    collection.addAll(entry.getValue());
                }
                catch (SecurityException e) {}
            }
        }
    }

    void addImportedEndpoints(Collection<ImportReference> collection) {

        SecurityManager securityManager = System.getSecurityManager();
        String frameworkUUID = ServiceUtil.getFrameworkUUID(getBundleContext());
        synchronized (m_importedEndpoints) {
            for (Entry<EndpointDescription, Set<ImportedEndpointImpl>> entry : m_importedEndpoints.entrySet()) {
                try {
                    if (securityManager != null) {
                        securityManager.checkPermission(new EndpointPermission(entry.getKey(), frameworkUUID, READ));
                    }
                    collection.addAll(entry.getValue());
                }
                catch (SecurityException e) {}
            }
        }
    }

    void exportedEndpointUpdated(final ExportedEndpointImpl exportedEndpoint) {

        EndpointDescription endpoint = exportedEndpoint.getExportedEndpoint(true);
        synchronized (m_exportedEndpoints) {
            Set<ExportedEndpointImpl> exportedEndpoints = m_exportedEndpoints.get(endpoint);
            if (exportedEndpoints != null) {
                exportedEndpoints.remove(exportedEndpoint);
                exportedEndpoints.add(exportedEndpoint);
            }
        }

        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                getEventsHandler().emitEvent(EXPORT_UPDATE, getBundleContext().getBundle(), exportedEndpoint,
                    exportedEndpoint.getException(true));
                return null;
            }
        });
    }

    void exportedEndpointClosed(final ExportedEndpointImpl exportedEndpoint) {

        EndpointDescription endpoint = exportedEndpoint.getExportedEndpoint(true);
        synchronized (m_exportedEndpoints) {
            Set<ExportedEndpointImpl> exportedEndpoints = m_exportedEndpoints.get(endpoint);
            if (exportedEndpoints != null) {
                exportedEndpoints.remove(exportedEndpoint);
                if (exportedEndpoints.isEmpty()) {
                    m_exportedEndpoints.remove(endpoint);
                }
            }
        }

        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                getEventsHandler().emitEvent(EXPORT_UNREGISTRATION, getBundleContext().getBundle(),
                    exportedEndpoint, exportedEndpoint.getException(true));
                return null;
            }
        });
    }

    void importedEndpointUpdated(final ImportedEndpointImpl importedEndpoint) {

        EndpointDescription endpoint = importedEndpoint.getImportedEndpoint(true);
        synchronized (m_importedEndpoints) {
            Set<ImportedEndpointImpl> importedEndpoints = m_importedEndpoints.get(endpoint);
            if (importedEndpoints != null) {
                importedEndpoints.remove(importedEndpoint);
                importedEndpoints.add(importedEndpoint);
            }
        }

        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                getEventsHandler().emitEvent(IMPORT_UPDATE, getBundleContext().getBundle(), importedEndpoint,
                    importedEndpoint.getException(true));
                return null;
            }
        });
    }

    void importedEndpointClosed(final ImportedEndpointImpl importedEndpoint) {

        EndpointDescription endpoint = importedEndpoint.getImportedEndpoint(true);
        synchronized (m_importedEndpoints) {
            Set<ImportedEndpointImpl> importedEndpoints = m_importedEndpoints.get(endpoint);
            if (importedEndpoints != null) {
                importedEndpoints.remove(importedEndpoint);
                if (importedEndpoints.isEmpty()) {
                    m_importedEndpoints.remove(endpoint);
                }
            }
        }

        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                getEventsHandler().emitEvent(IMPORT_UNREGISTRATION, getBundleContext().getBundle(),
                    importedEndpoint, importedEndpoint.getException(true));
                return null;
            }
        });
    }

    EventsHandlerImpl getEventsHandler() {
        return m_manager.getEventsHandler();
    }

    HttpServerEndpointHandler getServerEndpointHandler() {
        return m_manager.getServerEndpointHandler();
    }

    EndpointDescription createEndpointDescription(ServiceReference<?> reference, Map<String, ?> extraProperties) {
        return createEndpointDescription(UUID.randomUUID().toString(), reference, extraProperties);
    }

    EndpointDescription createEndpointDescription(String endpointId, ServiceReference<?> reference,
        Map<String, ?> extraProperties) {
        return createEndpointDescription(endpointId, getMergedProperties(reference, extraProperties));
    }

    private Class<?>[] loadEndpointInterfaces(ServiceReference<?> reference, EndpointDescription endpoint) {

        BundleContext context = getBundleContext();
        Object service = context.getService(reference);
        if (service == null) {
            logWarning(
                "Export failed. Unable to aquire service for reference (%s) and endpoint (%s). Service Registration closed?",
                reference, endpoint);
            return null;
        }

        Map<String, Class<?>> interfaces = collectInterfaces(service.getClass());
        context.ungetService(reference);

        String[] endpointInterfaces = getStringPlusValue(endpoint.getProperties().get(OBJECTCLASS));
        Class<?>[] exportedInterfaces = new Class<?>[endpointInterfaces.length];
        for (int i = 0; i < endpointInterfaces.length; i++) {
            exportedInterfaces[i] = interfaces.get(endpointInterfaces[i]);
            if (exportedInterfaces[i] == null) {
                logWarning(
                    "Export failed. Unable load exported interface (%s) from bundle for reference (%s) and endpoint (%s)",
                    exportedInterfaces[i], reference, endpoint);
                return null;
            }
        }
        return exportedInterfaces;
    }

    private EndpointDescription createEndpointDescription(String endpointId, Map<String, Object> properties) {
        String[] configurationTypes = getStringPlusValue(properties.get(SERVICE_EXPORTED_CONFIGS));
        if (configurationTypes.length > 0) {
            if (!Arrays.asList(configurationTypes).contains(CONFIGURATION_TYPE)) {
                logDebug("Can not export service (no supported configuration type specified): %s", properties);
                return null;
            }
        }

        // FIXME This is a hotfix to satisfy the OSGi CT that asserts that an exceptions is raised when it passes
        // garbage into configuration type specific parameters.
        if (properties.get(ENDPOINT_URL) != null) {
            throw new IllegalArgumentException("Can not export service (illegal configuration type parameter)");
        }

        if (properties.get(SERVICE_EXPORTED_INTERFACES) == null) {
            logWarning("Can not export service (no exported interfaces): %s", properties);
            throw new IllegalArgumentException("Can not export service (no exported interfaces)");
        }

        String[] exportedInterfaces = getExportedInterfaces(properties);
        if (exportedInterfaces.length == 0) {
            logWarning("Can not export service (no exported interfaces): %s", properties);
            return null;
        }

        String[] exportedIntents = getExportedIntents(properties);
        if (!isExportedIntentsSupported(exportedIntents)) {
            logDebug("Can not export service (unsupported intent specified): %s", properties);
            return null;
        }

        properties.put(ENDPOINT_ID, endpointId);
        properties.put(OBJECTCLASS, exportedInterfaces);
        properties.put(SERVICE_IMPORTED_CONFIGS, new String[] { CONFIGURATION_TYPE });
        properties.put(SERVICE_INTENTS, exportedIntents);
        properties.put(ENDPOINT_SERVICE_ID, properties.get(SERVICE_ID));
        properties.put(ENDPOINT_FRAMEWORK_UUID, ServiceUtil.getFrameworkUUID(getBundleContext()));

        URL endpointURL = m_manager.getServerEndpointHandler().getEndpointURL(endpointId);
        properties.put(ENDPOINT_URL, endpointURL.toString());

        return new EndpointDescription(properties);
    }

    /**
     * Returns a list of exported interface names as declared by the SERVICE_EXPORTED_INTERFACES property
     * using the following rules.
     * <ul>
     * <li>A single value of '*' means the OBJECTCLASS must be used </li>
     * <li>Any interface must be listed in the OBJECTCLASS</li>
     * </ul>
     * 
     * @param exportProperties the map of export properties
     * @return a list of exported interfaces names
     * @throws IllegalArgumentException if an interfaces is listed as export but it is not in the OBJECTCLASS.
     */
    private String[] getExportedInterfaces(Map<String, ?> exportProperties) {
        String[] providedInterfaces = getStringPlusValue(exportProperties.get(OBJECTCLASS));
        String[] exportedInterfaces = getStringPlusValue(exportProperties.get(SERVICE_EXPORTED_INTERFACES));
        if (exportedInterfaces.length == 1 && exportedInterfaces[0].equals("*")) {
            exportedInterfaces = providedInterfaces;
        }
        else {
            for (String exportedInterface : exportedInterfaces) {
                if ("*".equals(exportedInterface)) {
                    throw new IllegalArgumentException(
                        "Cannot accept wildcard together with other exported interfaces!");
                }
                boolean contained = false;
                for (String providedInterface : providedInterfaces) {
                    contained |= providedInterface.equals(exportedInterface);
                }
                if (!contained) {
                    logWarning("Exported interface %s not implemented by service: %s", exportedInterface,
                        providedInterfaces);
                    return new String[] {};
                }
            }
        }
        return exportedInterfaces;
    }

    /**
     * Returns an array exported intents based on the {@link SERVICE_EXPORTED_INTENTS} and {@link SERVICE_EXPORTED_INTENTS_EXTRA}<br>
     * property values as well as the default {@link HTTP_PASSBYVALYE_INTENT}.
     * 
     * @param properties the properties
     * @return an array of intents
     */
    private static String[] getExportedIntents(Map<String, Object> properties) {
        Object exportedIntents = properties.get(SERVICE_EXPORTED_INTENTS);
        Object exportedIntentsExtra = properties.get(SERVICE_EXPORTED_INTENTS_EXTRA);
        if (exportedIntents == null && exportedIntentsExtra == null) {
            return new String[] { PASSBYVALYE_INTENT };
        }
        Set<String> set = new HashSet<String>();
        if (exportedIntents != null) {
            for (String exportedIntent : getStringPlusValue(exportedIntents)) {
                set.add(exportedIntent);
            }
        }
        if (exportedIntentsExtra != null) {
            for (String exportedIntent : getStringPlusValue(exportedIntentsExtra)) {
                set.add(exportedIntent);
            }
        }
        set.add(PASSBYVALYE_INTENT);
        return set.toArray(new String[set.size()]);
    }

    /**
     * Returns a map of merged properties from the specified Service Reference and an optional map with
     * extra properties. Merging is done under the following rules:
     * <ul>
     * <li>Properties with a key starting with a '.' are private and thus ignored</li>
     * <li>Extra properties override service properties irrespective of casing</li>
     * </ul>
     * 
     * @param reference a Service Reference
     * @param extraProperties a map of extra properties, can be {@link null}
     * @return a map of merged properties
     */
    private static Map<String, Object> getMergedProperties(ServiceReference<?> reference, Map<String, ?> extraProperties) {
        Map<String, Object> serviceProperties = new HashMap<String, Object>();
        for (String propertyKey : reference.getPropertyKeys()) {
            if (propertyKey.startsWith(".")) {
                continue;
            }
            serviceProperties.put(propertyKey, reference.getProperty(propertyKey));
        }

        if (extraProperties == null) {
            return serviceProperties;
        }
        Set<String> removeServicePropertyKeys = new HashSet<String>();
        for (String extraPropertyKey : extraProperties.keySet()) {
            if (extraPropertyKey.startsWith(".") || extraPropertyKey.equalsIgnoreCase(SERVICE_ID)
                || extraPropertyKey.equalsIgnoreCase(OBJECTCLASS)) {
                continue;
            }

            for (String servicePropertyKey : serviceProperties.keySet()) {
                if (servicePropertyKey.equalsIgnoreCase(extraPropertyKey)) {
                    removeServicePropertyKeys.add(servicePropertyKey);
                }
            }
            for (String removeServicePropertyKey : removeServicePropertyKeys) {
                serviceProperties.remove(removeServicePropertyKey);
            }
            removeServicePropertyKeys.clear();
            serviceProperties.put(extraPropertyKey, extraProperties.get(extraPropertyKey));
        }
        return serviceProperties;
    }

    /**
     * Determines whether an array of intents is supported by this implementation.
     * 
     * @param exportedIntents the intents
     * @return {@code true} if supported, {@code false} otherwise.
     */
    private static boolean isExportedIntentsSupported(String[] exportedIntents) {
        if (exportedIntents == null) {
            return false;
        }
        for (String exportedIntent : exportedIntents) {
            if (!MY_SUPPORTED_INTENTS_SET.contains(exportedIntent)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines whether an {@link EndpointDescription} contains a well-formed {@code ENDPOINT_URL} property.
     * 
     * @param endpoint the endpoint
     * @return {@code true} if valid, {@code false} otherwise.
     */
    private static boolean hasValidServiceLocation(EndpointDescription endpoint) {
        Object serviceLocation = endpoint.getProperties().get(ENDPOINT_URL);
        if (serviceLocation == null || !(serviceLocation instanceof String)) {
            return false;
        }
        try {
            new URL((String) serviceLocation);
        }
        catch (MalformedURLException e) {
            return false;
        }
        return true;
    }

    /**
     * Determines whether an {@link EndpointDescription} lists interfaces that are available to this
     * Remote Service Admin bundle.<br/><br/>
     * 
     * Note that the loading of classes effectively triggers dynamic imports, wiring this bundle to a
     * provider. Even though importing endpoints are registered using a {@link ServiceFactory} this seems
     * to be required for Equinox 3.10 to consider them to be assignable to the interfaces.
     * 
     * @param endpoint the endpoint
     * @return {@code true} if valid, {@code false} otherwise.
     */
    private static boolean hasAvailableInterfaces(EndpointDescription endpoint) {
        List<String> interfaces = endpoint.getInterfaces();
        if (interfaces == null || interfaces.isEmpty()) {
            return false;
        }
        try {
            for (String iface : interfaces) {
                RemoteServiceAdminImpl.class.getClassLoader().loadClass(iface);
            }
        }
        catch (ClassNotFoundException e) {
            return false;
        }
        return true;
    }

    private static Map<String, Class<?>> collectInterfaces(Class<?> clazz) {
        Map<String, Class<?>> accumulator = new HashMap<String, Class<?>>();
        collectInterfaces(clazz, accumulator);
        return accumulator;
    }

    private static void collectInterfaces(Class<?> clazz, Map<String, Class<?>> accumulator) {
        for (Class<?> iface : clazz.getInterfaces()) {
            if (!accumulator.containsKey(iface.getName())) {
                accumulator.put(iface.getName(), iface);
                collectInterfaces(iface, accumulator);
            }
        }
        Class<?> parent = clazz.getSuperclass();
        if (parent != null) {
            collectInterfaces(parent, accumulator);
        }
    }
}
