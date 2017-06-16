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
package org.amdatu.remote.discovery.zookeeper;

import static org.amdatu.remote.ServiceUtil.getConfigIntValue;
import static org.amdatu.remote.ServiceUtil.getConfigStringValue;
import static org.amdatu.remote.discovery.DiscoveryUtil.createEndpointListenerServiceProperties;

import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointEventListener;
import org.osgi.service.remoteserviceadmin.EndpointListener;

/**
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 * 
 */
@SuppressWarnings("deprecation")
public class Activator extends DependencyActivatorBase implements ZookeeperDiscoveryConfiguration, ManagedService {

    public static final String CONFIG_PID = "org.amdatu.remote.discovery.zookeeper";
    public static final String CONFIG_HOST_KEY = CONFIG_PID + ".host";
    public static final String CONFIG_PORT_KEY = CONFIG_PID + ".port";
    public static final String CONFIG_PATH_KEY = CONFIG_PID + ".path";
    public static final String CONFIG_SCHEDULE_KEY = CONFIG_PID + ".schedule";
    public static final String CONFIG_CONNECT_TIMEOUT_KEY = CONFIG_PID + ".connecttimeout";
    public static final String CONFIG_READ_TIMEOUT_KEY = CONFIG_PID + ".readtimeout";

    public static final String CONFIG_CONNECTSTRING_KEY = CONFIG_PID + ".connectstring";
    public static final String CONFIG_ROOTPATH_KEY = CONFIG_PID + ".rootpath";
    public static final String CONFIG_TICKTIME_KEY = CONFIG_PID + ".ticktime";

    private volatile BundleContext m_context;
    private volatile DependencyManager m_manager;

    private volatile Component m_configuration;
    private volatile Component m_discovery;

    private volatile URL m_baseUrl;
    private volatile int m_schedule;
    private volatile int m_connectTimeout;
    private volatile int m_readTimeout;

    private volatile String m_connectString;
    private volatile String m_rootPath;
    private volatile int m_tickTime;

    @Override
    public void init(BundleContext context, DependencyManager manager) throws Exception {

        m_context = context;
        m_manager = manager;

        URL baseUrl = getConfiguredBaseUrl(null);
        int schedule = getConfiguredPollSchedule(null);
        int connectTimeout = getConfigIntValue(context, CONFIG_CONNECT_TIMEOUT_KEY, null, DEFAULT_CONNECT_TIMEOUT);
        int readTimeout = getConfigIntValue(context, CONFIG_READ_TIMEOUT_KEY, null, DEFAULT_READ_TIMEOUT);

        String connectString = getConfiguredConnectString(null);
        String rootPath = getConfiguredRootPath(null);
        int tickTime = getConfiguredTickTime(null);

        m_baseUrl = baseUrl;
        m_schedule = schedule;
        m_connectTimeout = connectTimeout;
        m_readTimeout = readTimeout;

        m_connectString = connectString;
        m_rootPath = rootPath;
        m_tickTime = tickTime;

        if (!m_connectString.equals("")) {
            registerDiscoveryService();
        }
        registerConfigurationService();
    }

    @Override
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {

        unregisterConfigurationService();
        unregisterDiscoveryService();
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {

        BundleContext context = getBundleContext();

        try {
            URL baseUrl = getConfiguredBaseUrl(properties);
            int schedule = getConfiguredPollSchedule(properties);
            int connectTimeout =
                getConfigIntValue(context, CONFIG_CONNECT_TIMEOUT_KEY, properties, DEFAULT_CONNECT_TIMEOUT);
            int readTimeout = getConfigIntValue(context, CONFIG_READ_TIMEOUT_KEY, properties, DEFAULT_READ_TIMEOUT);

            String connectString = getConfiguredConnectString(properties);
            String rootPath = getConfiguredRootPath(properties);
            int tickTime = getConfiguredTickTime(properties);

            m_connectTimeout = connectTimeout;
            m_readTimeout = readTimeout;

            if (!baseUrl.equals(m_baseUrl) || m_schedule != schedule || !m_connectString.equals(connectString)
                || !m_rootPath.equals(rootPath) || m_tickTime != tickTime) {
                unregisterDiscoveryService();
                m_baseUrl = baseUrl;
                m_schedule = schedule;
                m_connectString = connectString;
                m_rootPath = rootPath;
                m_tickTime = tickTime;
                if (!m_connectString.equals("")) {
                    registerDiscoveryService();
                }
            }
        }
        catch (Exception e) {
            throw new ConfigurationException("unknown", e.getMessage(), e);
        }
    }

    private void registerDiscoveryService() {

        Properties properties =
            createEndpointListenerServiceProperties(m_manager.getBundleContext(),
                ZookeeperEndpointDiscovery.DISCOVERY_TYPE);

        ZookeeperEndpointDiscovery discovery =
            new ZookeeperEndpointDiscovery(this);

        Component component = createComponent()
            .setInterface(new String[] { EndpointEventListener.class.getName(), EndpointListener.class.getName() },
                properties)
            .setImplementation(discovery)
            .add(createServiceDependency()
                .setService(HttpService.class)
                .setRequired(true))
            .add(createServiceDependency()
                .setService(EndpointEventListener.class)
                .setCallbacks("eventListenerAdded", "eventListenerModified", "eventListenerRemoved")
                .setRequired(false))
            .add(createServiceDependency()
                .setService(EndpointListener.class)
                .setCallbacks("listenerAdded", "listenerModified", "listenerRemoved")
                .setRequired(false))
            .add(createServiceDependency()
                .setService(LogService.class)
                .setRequired(false));

        m_discovery = component;
        m_manager.add(m_discovery);
    }

    private void unregisterDiscoveryService() {

        Component component = m_discovery;
        m_discovery = null;
        if (component != null) {
            m_manager.remove(component);
        }
    }

    private void registerConfigurationService() {

        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(Constants.SERVICE_PID, CONFIG_PID);

        Component component = createComponent()
            .setInterface(ManagedService.class.getName(), properties)
            .setImplementation(this)
            .setAutoConfig(BundleContext.class, false)
            .setAutoConfig(DependencyManager.class, false)
            .setAutoConfig(Component.class, false);

        m_configuration = component;
        m_manager.add(component);
    }

    private void unregisterConfigurationService() {

        Component component = m_configuration;
        m_configuration = null;
        if (component != null) {
            m_manager.remove(component);
        }
    }

    private String getConfiguredConnectString(Dictionary<String, ?> properties) throws ConfigurationException {
        return getConfigStringValue(m_context, CONFIG_CONNECTSTRING_KEY, properties, "");
    }

    private String getConfiguredRootPath(Dictionary<String, ?> properties) throws ConfigurationException {
        return getConfigStringValue(m_context, CONFIG_ROOTPATH_KEY, properties, "/");
    }

    private int getConfiguredTickTime(Dictionary<String, ?> properties) throws ConfigurationException {
        return getConfigIntValue(m_context, CONFIG_TICKTIME_KEY, properties, 2000);
    }

    private int getConfiguredPollSchedule(Dictionary<String, ?> properties) throws ConfigurationException {
        return getConfigIntValue(m_context, CONFIG_SCHEDULE_KEY, properties, 10);
    }

    private URL getConfiguredBaseUrl(Dictionary<String, ?> properties) throws ConfigurationException {

        String host = getConfigStringValue(m_context, CONFIG_HOST_KEY, properties, null);
        if (host == null) {
            // AMDATURS-32: it makes no sense to bind to the localhost by default...
            host = getConfigStringValue(m_context, "org.apache.felix.http.host", properties, "0.0.0.0");
        }

        int port = getConfigIntValue(m_context, CONFIG_PORT_KEY, properties, -1);
        if (port == -1) {
            port = getConfigIntValue(m_context, "org.osgi.service.http.port", properties, 8080);
        }

        String path = getConfigStringValue(m_context, CONFIG_PATH_KEY, properties, CONFIG_PID);
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (!path.endsWith("/")) {
            path = path + "/";
        }

        try {
            return new URL("http", host, port, path);
        }
        catch (Exception e) {
            throw new ConfigurationException("unknown", e.getMessage(), e);
        }
    }

    @Override
    public URL getBaseUrl() {
        return m_baseUrl;
    }

    @Override
    public int getConnectTimeout() {
        return m_connectTimeout;
    }

    @Override
    public int getReadTimeout() {
        return m_readTimeout;
    }

    @Override
    public int getSchedule() {
        return m_schedule;
    }

    @Override
    public String getConnectString() {
        return m_connectString;
    }

    @Override
    public String getRootPath() {
        return m_rootPath;
    }

    @Override
    public int getTickTime() {
        return m_tickTime;
    }
}
