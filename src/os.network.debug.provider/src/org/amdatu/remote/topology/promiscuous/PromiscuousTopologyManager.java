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
package org.amdatu.remote.topology.promiscuous;

import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_FRAMEWORK_UUID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_INTERFACES;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.EXPORT_ERROR;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.EXPORT_UNREGISTRATION;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.IMPORT_ERROR;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.IMPORT_UNREGISTRATION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.amdatu.remote.AbstractEndpointPublishingComponent;
import org.amdatu.remote.ServiceUtil;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointEvent;
import org.osgi.service.remoteserviceadmin.EndpointEventListener;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;

/**
 * {@link PromiscuousTopologyManager} implements a <i>Topology Manager</i> with of a promiscuous strategy. It will import
 * any discovered remote endpoint and export any locally available exportable service that matches the whitelist
 * filters. These can be extended through configuration under {@link #SERVICE_PID} using properties {@link #IMPORTS_FILTER} and {@link #EXPORTS_FILTER}.<p>
 * 
 * imports filter: {@code (&(!(endpoint.framework.uuid=<local framework uuid>))(<configured imports filter>))} <br>
 * exports filter: {@code (&(service.exported.interfaces=*)(<configured exports filter>))}<p>
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
@SuppressWarnings("deprecation")
public final class PromiscuousTopologyManager extends AbstractEndpointPublishingComponent implements
    RemoteServiceAdminListener, EndpointEventListener, EndpointListener, ManagedService {

    public final static String SERVICE_PID = "org.amdatu.remote.topology.promiscuous";
    public final static String IMPORTS_FILTER = SERVICE_PID + ".imports";
    public final static String EXPORTS_FILTER = SERVICE_PID + ".exports";

    private static class ExportRecord {

        public ExportRecord(ExportRegistration registration) {

            this.registration = registration;
            this.exception = registration.getException();
            if (this.exception == null) {
                this.reference = this.registration.getExportReference();
                if (this.reference != null) {
                    this.endpoint = this.reference.getExportedEndpoint();
                }
            }
        }

        ExportRegistration registration;
        ExportReference reference;
        EndpointDescription endpoint;
        Throwable exception;
    }

    private static class ImportRecord {

        public ImportRecord(ImportRegistration registration) {

            this.registration = registration;
            this.exception = registration.getException();
            if (this.exception == null) {
                this.reference = this.registration.getImportReference();
                if (this.reference != null) {
                    this.endpoint = this.reference.getImportedEndpoint();
                }
            }
        }

        ImportRegistration registration;
        ImportReference reference;
        EndpointDescription endpoint;
        Throwable exception;
    }

    private final Set<ServiceReference<?>> m_exportableServices = new HashSet<ServiceReference<?>>();
    private final Map<ServiceReference<?>, Map<RemoteServiceAdmin, Set<ExportRecord>>> m_exportedServices =
        new HashMap<ServiceReference<?>, Map<RemoteServiceAdmin, Set<ExportRecord>>>();

    private final Set<EndpointDescription> m_importableServices = new HashSet<EndpointDescription>();
    private final Map<EndpointDescription, Map<RemoteServiceAdmin, Set<ImportRecord>>> m_importedServices =
        new HashMap<EndpointDescription, Map<RemoteServiceAdmin, Set<ImportRecord>>>();

    private final List<RemoteServiceAdmin> m_remoteServiceAdmins = new ArrayList<RemoteServiceAdmin>();

    private volatile Filter m_exportsFilter = null;
    private volatile Filter m_importsFilter = null;

    public PromiscuousTopologyManager() {
        super("topology", "promiscuous");
    }

    @Override
    public void updated(Dictionary<String, ?> configuration) throws ConfigurationException {
        String frameworkUUID = ServiceUtil.getFrameworkUUID(getBundleContext());

        String imports = String.format("(!(%s=%s))", ENDPOINT_FRAMEWORK_UUID, frameworkUUID);
        String exports = String.format("(%s=%s)", SERVICE_EXPORTED_INTERFACES, "*");

        if (configuration != null) {
            Object importsFilter = configuration.get(IMPORTS_FILTER);
            if (importsFilter != null && !"".equals(importsFilter.toString().trim())) {
                imports = String.format("(&%s%s)", imports, importsFilter);
            }

            Object exportsFilter = configuration.get(EXPORTS_FILTER);
            if (exportsFilter != null && !"".equals(exportsFilter.toString().trim())) {
                exports = String.format("(&%s%s)", exports, exportsFilter);
            }
        }

        final Filter exportsFilter;
        try {
            exportsFilter = getBundleContext().createFilter(exports);
        }
        catch (InvalidSyntaxException ex) {
            throw new ConfigurationException(EXPORTS_FILTER, "Invalid filter!");
        }

        final Filter importsFilter;
        try {
            importsFilter = getBundleContext().createFilter(imports);
        }
        catch (InvalidSyntaxException ex) {
            throw new ConfigurationException(IMPORTS_FILTER, "Invalid filter!");
        }

        executeTask(new Runnable() {
            @Override
            public void run() {
                if (m_exportsFilter == null || !m_exportsFilter.equals(exportsFilter)) {
                    m_exportsFilter = exportsFilter;
                    logInfo("Configured export filter updated: %s", exportsFilter);

                    for (ServiceReference<?> service : m_exportedServices.keySet()) {
                        if (!isWhitelisted(service)) {
                            removeExportedService(service);
                        }
                    }
                    for (ServiceReference<?> service : m_exportableServices) {
                        if (!m_exportedServices.containsKey(service) && isWhitelisted(service)) {
                            addExportedServices(service);
                        }
                    }
                }

                if (m_importsFilter == null || !m_importsFilter.equals(importsFilter)) {
                    m_importsFilter = importsFilter;
                    logInfo("Configured import filter updated: %s", importsFilter);

                    for (EndpointDescription endpoint : m_importedServices.keySet()) {
                        if (!isWhitelisted(endpoint)) {
                            removeImportedService(endpoint);
                        }
                    }
                    for (EndpointDescription endpoint : m_importableServices) {
                        if (!m_importedServices.containsKey(endpoint) && isWhitelisted(endpoint)) {
                            addImportedServices(endpoint);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void remoteAdminEvent(final RemoteServiceAdminEvent event) {
        executeTask(new Runnable() {
            @Override
            public void run() {

                switch (event.getType()) {
                    case EXPORT_UNREGISTRATION: {
                        removeExportedService(event.getExportReference());
                        break;
                    }
                    case EXPORT_ERROR: {
                        removeExportedService(event.getExportReference());
                        break;
                    }
                    case IMPORT_UNREGISTRATION: {
                        removeImportedService(event.getImportReference());
                        break;
                    }
                    case IMPORT_ERROR: {
                        removeImportedService(event.getImportReference());
                        break;
                    }
                    default:
                        break;
                }
            }
        });
    }

    @Override
    public void endpointChanged(final EndpointEvent event, final String matchedFilter) {
        final EndpointDescription endpoint = event.getEndpoint();
        executeTask(new Runnable() {
            @Override
            public void run() {
                switch (event.getType()) {
                    case EndpointEvent.ADDED:
                        logInfo("Importable endpoint added: %s", endpoint);
                        m_importableServices.add(endpoint);
                        if (isWhitelisted(endpoint)) {
                            addImportedServices(endpoint);
                        }
                        break;
                    case EndpointEvent.MODIFIED:
                        m_importableServices.remove(endpoint); // ensure the add
                        m_importableServices.add(endpoint);
                        if (isWhitelisted(endpoint)) {
                            logInfo("Importable endpoint modified: %s", endpoint);
                            updateImportedServices(endpoint);
                        }
                        else if (m_importedServices.containsKey(endpoint)) {
                            logInfo("Importable endpoint modified and removed: %s", endpoint);
                            removeImportedService(endpoint);
                        }
                        break;
                    case EndpointEvent.REMOVED:
                        logInfo("Importable endpoint removed: %s", endpoint);
                        m_importableServices.remove(endpoint);
                        if (isWhitelisted(endpoint)) {
                            removeImportedService(endpoint);
                        }
                        break;
                    case EndpointEvent.MODIFIED_ENDMATCH:
                        logInfo("Importable endpoint endmatched: %s", endpoint);
                        m_importableServices.remove(endpoint);
                        if (isWhitelisted(endpoint)) {
                            removeImportedService(endpoint);
                        }
                        break;
                    default:
                        logError("Recieved event with unknown type " + event.getType());
                }
            }
        });
    }

    @Override
    public void endpointAdded(final EndpointDescription endpoint, final String matchedFilter) {
        endpointChanged(new EndpointEvent(EndpointEvent.ADDED, endpoint), matchedFilter);
    }

    @Override
    public void endpointRemoved(final EndpointDescription endpoint, final String matchedFilter) {
        endpointChanged(new EndpointEvent(EndpointEvent.REMOVED, endpoint), matchedFilter);
    }

    // Dependency Manager callback method
    protected final void remoteServiceAdminAdded(final ServiceReference<RemoteServiceAdmin> reference,
        final RemoteServiceAdmin remoteServiceAdmin) {

        executeTask(new Runnable() {
            @Override
            public void run() {
                logInfo("Adding Remote Service Admin: %s", reference);
                m_remoteServiceAdmins.add(remoteServiceAdmin);
                addExportedServices(remoteServiceAdmin);
                addImportedServices(remoteServiceAdmin);
            }
        });
    }

    // Dependency Manager callback method
    protected final void remoteServiceAdminRemoved(final ServiceReference<RemoteServiceAdmin> reference,
        final RemoteServiceAdmin remoteServiceAdmin) {

        executeTask(new Runnable() {
            @Override
            public void run() {
                logInfo("Removing Remote Service Admin: %s", reference);
                m_remoteServiceAdmins.remove(remoteServiceAdmin);
                removeExportedServices(remoteServiceAdmin);
                removeImportedServices(remoteServiceAdmin);
            }
        });
    }

    // Dependency Manager callback method
    protected final void exportableServiceAdded(final ServiceReference<?> reference, final Object service) {

        executeTask(new Runnable() {
            @Override
            public void run() {
                m_exportableServices.add(reference);
                if (isWhitelisted(reference)) {
                    logInfo("Exported service added: %s", reference);
                    addExportedServices(reference);
                }
            }
        });
    }

    // Dependency Manager callback method
    protected final void exportableServiceModified(final ServiceReference<?> reference, final Object service) {

        executeTask(new Runnable() {
            @Override
            public void run() {
                m_exportableServices.remove(reference);
                m_exportableServices.add(reference);
                if (isWhitelisted(reference)) {
                    logInfo("Exported service modified: %s", reference);
                    updateExportedServices(reference);
                }
                else if (m_exportedServices.containsKey(reference)) {
                    logInfo("Exported service modified and removed: %s", reference);
                    removeExportedService(reference);
                }
            }
        });
    }

    // Dependency Manager callback method
    protected final void exportableServiceRemoved(final ServiceReference<?> reference, final Object service) {

        executeTask(new Runnable() {
            @Override
            public void run() {
                m_exportableServices.remove(reference);
                if (isWhitelisted(reference)) {
                    logInfo("Exported service removed: %s", reference);
                    removeExportedService(reference);
                }
            }
        });
    }

    /*
     * Imports
     */

    private void addImportedServices(final EndpointDescription endpoint) {
        assert !m_importedServices.containsKey(endpoint);
        Map<RemoteServiceAdmin, Set<ImportRecord>> adminRecords = new HashMap<RemoteServiceAdmin, Set<ImportRecord>>();
        m_importedServices.put(endpoint, adminRecords);
        for (RemoteServiceAdmin admin : m_remoteServiceAdmins) {
            Set<ImportRecord> records = new HashSet<ImportRecord>();
            adminRecords.put(admin, records);
            ImportRecord record = importService(admin, endpoint);
            if (record != null) {
                records.add(record);
            }
        }
    }

    private void addImportedServices(final RemoteServiceAdmin admin) {
        for (EndpointDescription endpoint : m_importedServices.keySet()) {
            Map<RemoteServiceAdmin, Set<ImportRecord>> adminRecords = m_importedServices.get(endpoint);
            assert !adminRecords.containsKey(admin);
            Set<ImportRecord> records = new HashSet<ImportRecord>();
            adminRecords.put(admin, records);
            ImportRecord record = importService(admin, endpoint);
            if (record != null) {
                records.add(record);
            }
        }
    }

    private void updateImportedServices(final EndpointDescription endpoint) {
        assert m_importedServices.containsKey(endpoint);
        // ensure the key gets updated by using remove/put
        Map<RemoteServiceAdmin, Set<ImportRecord>> adminRecords = m_importedServices.remove(endpoint);
        m_importedServices.put(endpoint, adminRecords);
        for (RemoteServiceAdmin admin : m_remoteServiceAdmins) {
            assert adminRecords.containsKey(admin);
            Set<ImportRecord> records = adminRecords.get(admin);
            if (records.isEmpty()) {
                // previous import failed
                ImportRecord record = importService(admin, endpoint);
                if (record != null) {
                    records.add(record);
                }
            }
            else {
                for (ImportRecord record : records) {
                    try {
                        if (record.registration.update(endpoint)) {
                            record.endpoint = endpoint;
                        }
                        else {
                            record.exception = record.registration.getException();
                            logWarning("Failed to update service import for endpoint: %s", record.exception, endpoint);
                        }
                    }
                    catch (Exception e) {
                        logWarning("Failed to update service import for endpoint: %s", e, endpoint);
                    }
                }
            }
        }
    }

    private ImportRecord importService(final RemoteServiceAdmin admin, final EndpointDescription endpoint) {
        ImportRecord record = null;
        try {
            ImportRegistration registration = admin.importService(endpoint);
            if (registration != null) {
                record = new ImportRecord(registration);
            }
            else {
                logWarning("Failed to import endpoint. RSA return null for: %s", endpoint);
            }
        }
        catch (Exception e) {
            logWarning("Failed to import endpoint. RSA threw exception for: %s", e, endpoint);
        }
        return record;
    }

    private void removeImportedService(final EndpointDescription endpoint) {
        assert m_importedServices.containsKey(endpoint);
        Map<RemoteServiceAdmin, Set<ImportRecord>> adminRecords = m_importedServices.remove(endpoint);
        for (Set<ImportRecord> records : adminRecords.values()) {
            for (ImportRecord record : records) {
                record.registration.close();
            }
        }
    }

    private void removeImportedServices(final RemoteServiceAdmin admin) {
        for (Map<RemoteServiceAdmin, Set<ImportRecord>> adminRecords : m_importedServices.values()) {
            assert adminRecords.containsKey(admin);
            Set<ImportRecord> records = adminRecords.remove(admin);
            for (ImportRecord record : records) {
                record.registration.close();
            }
        }
    }

    private void removeImportedService(final ImportReference reference) {
        ImportRecord removal = null;
        for (Map<RemoteServiceAdmin, Set<ImportRecord>> adminRecords : m_importedServices.values()) {
            for (Set<ImportRecord> records : adminRecords.values()) {
                for (ImportRecord record : records) {
                    if (record.reference == reference) {
                        removal = record;
                        break;
                    }
                }
                if (removal != null) {
                    records.remove(removal);
                    removal.registration.close();
                    if (removal.registration.getException() == null && removal.endpoint != null) {
                        endpointRemoved(removal.endpoint);
                    }
                    return;
                }
            }
        }
    }

    /*
     * Exports
     */

    private void addExportedServices(final ServiceReference<?> service) {
        assert !m_exportedServices.containsKey(service);
        Map<RemoteServiceAdmin, Set<ExportRecord>> adminRecords = new HashMap<RemoteServiceAdmin, Set<ExportRecord>>();
        m_exportedServices.put(service, adminRecords);
        for (RemoteServiceAdmin admin : m_remoteServiceAdmins) {
            Set<ExportRecord> records = new HashSet<ExportRecord>();
            adminRecords.put(admin, records);
            Set<ExportRecord> exports = exportService(admin, service);
            if (exports != null) {
                for (ExportRecord export : exports) {
                    records.add(export);
                    endpointAdded(export.endpoint);
                }
            }
        }
    }

    private void addExportedServices(final RemoteServiceAdmin admin) {
        for (ServiceReference<?> service : m_exportedServices.keySet()) {
            Map<RemoteServiceAdmin, Set<ExportRecord>> adminRecords = m_exportedServices.get(service);
            assert !adminRecords.containsKey(admin);
            Set<ExportRecord> records = new HashSet<ExportRecord>();
            adminRecords.put(admin, records);
            Set<ExportRecord> exports = exportService(admin, service);
            if (exports != null) {
                for (ExportRecord export : exports) {
                    records.add(export);
                    endpointAdded(export.endpoint);
                }
            }
        }
    }

    private void updateExportedServices(final ServiceReference<?> service) {
        assert m_exportedServices.containsKey(service);
        Map<RemoteServiceAdmin, Set<ExportRecord>> adminRecords = m_exportedServices.get(service);
        for (RemoteServiceAdmin admin : m_remoteServiceAdmins) {
            assert adminRecords.containsKey(admin);
            Set<ExportRecord> records = adminRecords.get(admin);
            if (records.isEmpty()) {
                // previous export failed
                Set<ExportRecord> exports = exportService(admin, service);
                if (exports != null) {
                    for (ExportRecord export : exports) {
                        records.add(export);
                        endpointAdded(export.endpoint);
                    }
                }
            }
            else {
                for (ExportRecord record : records) {
                    try {

                        EndpointDescription endpoint = record.registration.update(null);
                        if (endpoint != null) {
                            record.endpoint = endpoint;
                            endpointModified(endpoint);
                        }
                        else {
                            logWarning("Failed to update exported service %s", record.exception, record.registration);
                        }
                    }
                    catch (Exception e) {
                        logWarning("Failed to update service export for service: %s", e, service);
                    }
                }
            }
        }
    }

    private Set<ExportRecord> exportService(final RemoteServiceAdmin admin, final ServiceReference<?> service) {
        Set<ExportRecord> records = new HashSet<ExportRecord>();
        try {
            Collection<ExportRegistration> registrations = admin.exportService(service, null);
            for (ExportRegistration registration : registrations) {
                ExportRecord record = new ExportRecord(registration);
                records.add(record);
            }
        }
        catch (Exception e) {
            logWarning("Failed to export service for reference: %s", e, service);
        }
        return records;
    }

    private void removeExportedService(ServiceReference<?> service) {
        assert m_exportedServices.containsKey(service);
        Map<RemoteServiceAdmin, Set<ExportRecord>> adminRecords = m_exportedServices.remove(service);
        for (Set<ExportRecord> records : adminRecords.values()) {
            for (ExportRecord record : records) {
                record.registration.close();
                if (record.registration.getException() == null && record.endpoint != null) {
                    endpointRemoved(record.endpoint);
                }
            }
        }
    }

    private void removeExportedServices(RemoteServiceAdmin admin) {
        for (Map<RemoteServiceAdmin, Set<ExportRecord>> adminRecords : m_exportedServices.values()) {
            assert adminRecords.containsKey(admin);
            Set<ExportRecord> records = adminRecords.remove(admin);
            for (ExportRecord record : records) {
                record.registration.close();
                if (record.registration.getException() == null && record.endpoint != null) {
                    endpointRemoved(record.endpoint);
                }
            }
        }
    }

    private void removeExportedService(ExportReference reference) {
        ExportRecord removal = null;
        for (Map<RemoteServiceAdmin, Set<ExportRecord>> adminRecords : m_exportedServices.values()) {
            for (Set<ExportRecord> records : adminRecords.values()) {
                for (ExportRecord record : records) {
                    if (record.reference == reference) {
                        removal = record;
                        break;
                    }
                }
                if (removal != null) {
                    records.remove(removal);
                    removal.registration.close();
                    if (removal.registration.getException() == null && removal.endpoint != null) {
                        endpointRemoved(removal.endpoint);
                    }
                    return;
                }
            }
        }
    }

    /*
     * Filters
     */

    private boolean isWhitelisted(final ServiceReference<?> reference) {
        Filter exportsFilter = m_exportsFilter;
        return exportsFilter != null && exportsFilter.match(reference);
    }

    private boolean isWhitelisted(final EndpointDescription endpoint) {
        Filter importsFilter = m_importsFilter;
        return importsFilter != null && importsFilter.matches(endpoint.getProperties());
    }
}
