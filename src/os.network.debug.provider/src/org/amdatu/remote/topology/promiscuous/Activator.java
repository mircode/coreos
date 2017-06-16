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

import static org.amdatu.remote.ServiceUtil.getFrameworkUUID;
import static org.osgi.service.remoteserviceadmin.EndpointListener.ENDPOINT_LISTENER_SCOPE;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_FRAMEWORK_UUID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_INTERFACES;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointEventListener;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;

/**
 * Activator for the Amdatu Topology Manager service implementation.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
@SuppressWarnings("deprecation")
public class Activator extends DependencyActivatorBase {

    @Override
    public void init(BundleContext context, DependencyManager manager) throws Exception {

        String[] objectClass =
            new String[] { RemoteServiceAdminListener.class.getName(), EndpointEventListener.class.getName(),
                EndpointListener.class.getName(), ManagedService.class.getName() };

        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(Constants.SERVICE_PID, PromiscuousTopologyManager.SERVICE_PID);
        properties.put(ENDPOINT_LISTENER_SCOPE,
            "(!(" + ENDPOINT_FRAMEWORK_UUID + "=" + getFrameworkUUID(context) + "))");

        manager.add(
            createComponent()
                .setInterface(objectClass, properties)
                .setImplementation(PromiscuousTopologyManager.class)
                .add(createServiceDependency()
                    .setService(LogService.class)
                    .setRequired(false))
                .add(createServiceDependency()
                    .setService(RemoteServiceAdmin.class)
                    .setCallbacks("remoteServiceAdminAdded", "remoteServiceAdminRemoved")
                    .setRequired(false))
                .add(createServiceDependency()
                    .setService(null, "(" + SERVICE_EXPORTED_INTERFACES + "=*)")
                    .setCallbacks("exportableServiceAdded", "exportableServiceModified", "exportableServiceRemoved")
                    .setRequired(false))
                .add(createServiceDependency()
                    .setService(EndpointEventListener.class)
                    .setCallbacks("eventListenerAdded", "eventListenerModified", "eventListenerRemoved")
                    .setRequired(false))
                .add(createServiceDependency()
                    .setService(EndpointListener.class)
                    .setCallbacks("listenerAdded", "listenerModified", "listenerRemoved")
                    .setRequired(false))
            );
    }

    @Override
    public void destroy(BundleContext context, DependencyManager manager)
        throws Exception {
    }
}
