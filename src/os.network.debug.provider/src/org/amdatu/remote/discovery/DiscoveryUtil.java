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

import static org.amdatu.remote.ServiceUtil.getFrameworkUUID;
import static org.amdatu.remote.discovery.DiscoveryConstants.DISCOVERY;
import static org.amdatu.remote.discovery.DiscoveryConstants.DISCOVERY_TYPE;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_FRAMEWORK_UUID;

import java.util.Properties;

import org.osgi.framework.BundleContext;
import org.osgi.service.remoteserviceadmin.EndpointEventListener;
import org.osgi.service.remoteserviceadmin.EndpointListener;

/**
 * Collection of Discovery specific utility methods.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
@SuppressWarnings("deprecation")
public final class DiscoveryUtil {

    public static Properties createEndpointListenerServiceProperties(BundleContext context, String discoveryType) {
        Properties properties = new Properties();
        properties.put(DISCOVERY, true);
        properties.put(DISCOVERY_TYPE, discoveryType);
        properties.put(EndpointEventListener.ENDPOINT_LISTENER_SCOPE, createEndpointListenerScopeFilter(context));
        properties.put(EndpointListener.ENDPOINT_LISTENER_SCOPE, createEndpointListenerScopeFilter(context));
        return properties;
    }

    private static String createEndpointListenerScopeFilter(BundleContext bundleContext) {
        return "(&(" + OBJECTCLASS + "=*)(" + ENDPOINT_FRAMEWORK_UUID + "="
            + getFrameworkUUID(bundleContext) + "))";
    }

    private DiscoveryUtil() {
    }

}
