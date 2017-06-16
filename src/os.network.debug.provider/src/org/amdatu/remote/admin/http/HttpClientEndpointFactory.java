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
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * Provides a {@link ServiceFactory} that creates a real {@link HttpClientEndpoint} for each bundle that is getting the service.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class HttpClientEndpointFactory implements ServiceFactory<Object>, ClientEndpointProblemListener {

    private final URL m_serviceURL;
    private final List<String> m_interfaceNames;
    private ClientEndpointProblemListener m_problemListener;
    private HttpAdminConfiguration m_configuration;

    /**
     * Creates a new {@link HttpClientEndpointFactory} instance.
     */
    public HttpClientEndpointFactory(URL serviceURL, List<String> interfaceNames, HttpAdminConfiguration configuration) {
        m_serviceURL = serviceURL;
        m_interfaceNames = interfaceNames;
        m_configuration = configuration;
    }

    @Override
    public Object getService(Bundle bundle, ServiceRegistration<Object> registration) {
        Class<?>[] interfaceClasses = new Class<?>[m_interfaceNames.size()];
        for (int i = 0; i < interfaceClasses.length; i++) {
            String iface = m_interfaceNames.get(i);
            try {
                interfaceClasses[i] = bundle.loadClass(iface);
            }
            catch (ClassNotFoundException e) {
                return null;
            }
        }
        HttpClientEndpoint restEndpoint = new HttpClientEndpoint(m_serviceURL, m_configuration, interfaceClasses);
        restEndpoint.setProblemListener(this);
        return restEndpoint.getServiceProxy();
    }

    @Override
    public synchronized void handleEndpointError(Throwable exception) {
        if (m_problemListener != null) {
            m_problemListener.handleEndpointError(exception);
        }
    }

    @Override
    public synchronized void handleEndpointWarning(Throwable exception) {
        if (m_problemListener != null) {
            m_problemListener.handleEndpointWarning(exception);
        }
    }

    /**
     * @param problemListener the problem listener to set, can be <code>null</code>.
     */
    public synchronized void setProblemListener(ClientEndpointProblemListener problemListener) {
        m_problemListener = problemListener;
    }

    @Override
    public void ungetService(Bundle bundle, ServiceRegistration<Object> registration, Object service) {
        // Nop
    }
}
