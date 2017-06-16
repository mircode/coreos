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

import static org.amdatu.remote.ServiceUtil.getConfigIntValue;
import static org.amdatu.remote.ServiceUtil.getConfigStringValue;
import static org.amdatu.remote.admin.http.HttpAdminConstants.CONNECT_TIMEOUT_CONFIG_KEY;
import static org.amdatu.remote.admin.http.HttpAdminConstants.PATH_CONFIG_KEY;
import static org.amdatu.remote.admin.http.HttpAdminConstants.READ_TIMEOUT_CONFIG_KEY;
import static org.amdatu.remote.admin.http.HttpAdminConstants.SERVICE_PID;
import static org.amdatu.remote.admin.http.HttpAdminConstants.SUPPORTED_CONFIGURATION_TYPES;
import static org.amdatu.remote.admin.http.HttpAdminConstants.SUPPORTED_INTENTS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.REMOTE_CONFIGS_SUPPORTED;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.REMOTE_INTENTS_SUPPORTED;

import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;

/**
 * Activator and configuration manager for the Amdatu HTTP Remote Service Admin service implementation.
 * <p>
 * Configuration can be provided through cm as well as system properties. The former take precedence and
 * in addition some fallbacks and defaults are provided. See {@link HttpAdminConstants} for supported
 * configuration properties.
 * <p>
 * Note that any effective configuration change will close all existing import- and export registrations.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class Activator extends DependencyActivatorBase implements ManagedService, HttpAdminConfiguration {
    private static final int DEFAULT_CONNECT_TIMEOUT = 5000;
    private static final int DEFAULT_READ_TIMEOUT = 60000;

    private volatile DependencyManager m_dependencyManager;
    private volatile Component m_configurationComponent;
    private volatile Component m_factoryComponent;
    private volatile URL m_baseUrl;
    private volatile int m_connectTimeout;
    private volatile int m_readTimeout;
    private volatile Dictionary<String, ?> m_properties;

    @Override
    public void init(BundleContext context, DependencyManager manager) throws Exception {
        m_dependencyManager = manager;

        int connectTimeout = getConfigIntValue(context, CONNECT_TIMEOUT_CONFIG_KEY, null, DEFAULT_CONNECT_TIMEOUT);
        int readTimeout = getConfigIntValue(context, READ_TIMEOUT_CONFIG_KEY, null, DEFAULT_READ_TIMEOUT);

        try {
            m_baseUrl = parseConfiguredBaseUrl(context);
            m_connectTimeout = connectTimeout;
            m_readTimeout = readTimeout;
            registerFactoryService();
            registerConfigurationService();
        }
        catch (Exception e) {
            throw new ConfigurationException("base url", "invalid url", e);
        }
    }

    @Override
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {

        unregisterConfigurationService();
        unregisterFactoryService();
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        m_properties = properties;

        BundleContext context = getBundleContext();
        // first parse timeout to local variables, in order to make this method "transactional"
        // assign values to fields after baseUrl was successfully
        int connectTimeout = getConfigIntValue(context, CONNECT_TIMEOUT_CONFIG_KEY, m_properties, DEFAULT_CONNECT_TIMEOUT);
        int readTimeout = getConfigIntValue(context, READ_TIMEOUT_CONFIG_KEY, m_properties, DEFAULT_READ_TIMEOUT);
        URL baseUrl = parseConfiguredBaseUrl(context);

        try {
            m_connectTimeout = connectTimeout;
            m_readTimeout = readTimeout;

            if (!baseUrl.equals(m_baseUrl)) {
                unregisterFactoryService();
                m_baseUrl = baseUrl;
                registerFactoryService();
            }
        }
        catch (Exception e) {
            throw new ConfigurationException("base url", "invalid url", e);
        }
    }

    private void registerConfigurationService() {
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(Constants.SERVICE_PID, HttpAdminConstants.SERVICE_PID);

        Component component = createComponent()
            .setInterface(ManagedService.class.getName(), properties)
            .setImplementation(this)
            .setAutoConfig(DependencyManager.class, false)
            .setAutoConfig(Component.class, false);

        m_configurationComponent = component;
        m_dependencyManager.add(component);
    }

    private void unregisterConfigurationService() {
        Component component = m_configurationComponent;
        m_configurationComponent = null;
        if (component != null) {
            m_dependencyManager.remove(component);
        }
    }

    private void registerFactoryService() {
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(REMOTE_CONFIGS_SUPPORTED, SUPPORTED_CONFIGURATION_TYPES);
        properties.put(REMOTE_INTENTS_SUPPORTED, SUPPORTED_INTENTS);

        RemoteServiceAdminFactory factory = new RemoteServiceAdminFactory(this);

        Component component = createComponent()
            .setInterface(RemoteServiceAdmin.class.getName(), properties)
            .setImplementation(factory)
            .add(createServiceDependency()
                .setService(HttpService.class)
                .setRequired(true))
            .add(createServiceDependency()
                .setService(LogService.class)
                .setRequired(false))
            .add(createServiceDependency()
                .setService(RemoteServiceAdminListener.class)
                .setCallbacks(factory.getEventsHandler(), "listenerAdded", "listenerRemoved")
                .setRequired(false))
            .add(createServiceDependency()
                .setService(EventAdmin.class)
                .setCallbacks(factory.getEventsHandler(), "eventAdminAdded", "eventAdminRemoved")
                .setRequired(false));

        m_factoryComponent = component;
        m_dependencyManager.add(component);
    }

    private void unregisterFactoryService() {
        Component component = m_factoryComponent;
        m_factoryComponent = null;
        if (component != null) {
            m_dependencyManager.remove(component);
        }
    }

    private URL parseConfiguredBaseUrl(BundleContext context) throws ConfigurationException {
        String host = getConfigStringValue(context, HttpAdminConstants.HOST_CONFIG_KEY, m_properties, null);
        if (host == null) {
            host = getConfigStringValue(context, "org.apache.felix.http.host", m_properties, "localhost");
        }

        int port = getConfigIntValue(context, HttpAdminConstants.PORT_CONFIG_KEY, m_properties, -1);
        if (port == -1) {
            port = getConfigIntValue(context, "org.osgi.service.http.port", m_properties, 8080);
        }

        String path = getConfigStringValue(context, PATH_CONFIG_KEY, m_properties, SERVICE_PID);
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
}
