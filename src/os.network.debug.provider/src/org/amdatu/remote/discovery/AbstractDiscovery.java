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

import static org.amdatu.remote.EndpointUtil.computeHash;

import java.util.HashMap;
import java.util.Map;

import org.amdatu.remote.AbstractEndpointPublishingComponent;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointEvent;
import org.osgi.service.remoteserviceadmin.EndpointEventListener;
import org.osgi.service.remoteserviceadmin.EndpointListener;

/**
 * Base class for a Discovery Service that handles endpoint registration as well as listener tracking
 * and invocation.<br/><br/>
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
@SuppressWarnings("deprecation")
public abstract class AbstractDiscovery extends AbstractEndpointPublishingComponent implements EndpointEventListener,
    EndpointListener {

    private final Map<EndpointDescription, String> m_discoveredEndpoints = new HashMap<EndpointDescription, String>();

    public AbstractDiscovery(String name) {
        super("discovery", name);
    }

    @Override
    protected void startComponent() throws Exception {
        super.startComponent();
    }

    @Override
    protected void stopComponent() throws Exception {
        super.stopComponent();
    }

    @Override
    public void endpointChanged(final EndpointEvent event, final String matchedFilter) {
        switch (event.getType()) {
            case EndpointEvent.ADDED:
                executeTask(new Runnable() {

                    @Override
                    public void run() {
                        logInfo("Added local endpoint: %s", event.getEndpoint());
                        addPublishedEndpoint(event.getEndpoint(), matchedFilter);
                    }
                });
                break;
            case EndpointEvent.REMOVED:
                executeTask(new Runnable() {

                    @Override
                    public void run() {
                        logInfo("Removed local endpoint: %s", event.getEndpoint());
                        removePublishedEndpoint(event.getEndpoint(), matchedFilter);
                    }
                });
                break;
            case EndpointEvent.MODIFIED:
                executeTask(new Runnable() {

                    @Override
                    public void run() {
                        logInfo("Modified local endpoint: %s", event.getEndpoint());
                        modifyPublishedEndpoint(event.getEndpoint(), matchedFilter);
                    }
                });
                break;
            case EndpointEvent.MODIFIED_ENDMATCH:
                executeTask(new Runnable() {

                    @Override
                    public void run() {
                        logInfo("Endmatched local endpoint: %s", event.getEndpoint());
                        removePublishedEndpoint(event.getEndpoint(), matchedFilter);
                    }
                });
                break;
            default:
                throw new IllegalStateException("Recieved event with unknown type " + event.getType());
        }
    }

    @Override
    public void endpointAdded(final EndpointDescription endpoint, final String matchedFilter) {
        executeTask(new Runnable() {

            @Override
            public void run() {
                logInfo("Added local endpoint: %s", endpoint);
                addPublishedEndpoint(endpoint, matchedFilter);
            }
        });
    }

    @Override
    public void endpointRemoved(final EndpointDescription endpoint, final String matchedFilter) {
        executeTask(new Runnable() {

            @Override
            public void run() {
                logInfo("Removed local endpoint: %s", endpoint);
                removePublishedEndpoint(endpoint, matchedFilter);
            }
        });
    }

    /**
     * Register a newly discovered remote service and invoke relevant listeners. Concrete implementations must
     * call this method for every applicable remote registration they discover.
     * 
     * @param endpoint The service Endpoint Description
     */
    protected final void addDiscoveredEndpoint(final EndpointDescription endpoint) {
        executeTask(new Runnable() {

            @Override
            public void run() {
                String newhash = computeHash(endpoint);
                String oldhash = m_discoveredEndpoints.get(endpoint);
                if (oldhash == null) {
                    logInfo("Adding remote endpoint: %s", endpoint);
                    m_discoveredEndpoints.put(endpoint, newhash);
                    endpointAdded(endpoint);
                }
                else if (!oldhash.equals(newhash)) {
                    logInfo("Mofifying remote endpoint: %s", endpoint);
                    m_discoveredEndpoints.put(endpoint, newhash);
                    endpointModified(endpoint);
                }

            }
        });
    }

    /**
     * Unregister a previously discovered remote service endPoint and invoke relevant listeners. Concrete
     * implementations must call this method for every applicable remote registration that disappears.
     * 
     * @param endpoint The service Endpoint Description
     */
    protected final void removeDiscoveredEndpoint(final EndpointDescription endpoint) {
        executeTask(new Runnable() {

            @Override
            public void run() {
                logInfo("Removed remote endpoint: %s", endpoint);
                m_discoveredEndpoints.remove(endpoint);
                endpointRemoved(endpoint);
            }
        });
    }

    /**
     * Modifies a previously discovered remote service endPoint and invoke relevant listeners. Concrete
     * implementations must call this method for every applicable remote registration that disappears.
     * 
     * @param endpoint The service Endpoint Description
     */
    protected final void modifyDiscoveredEndpoint(EndpointDescription endpoint) {
        addDiscoveredEndpoint(endpoint);
    }

    /**
     * Called when an exported service is published. The concrete implementation is responsible for registering
     * the service in its service registry.
     * 
     * @param endpoint The service Endpoint Description
     * @param matchedFilter The matched filter
     */
    protected abstract void addPublishedEndpoint(EndpointDescription endpoint, String matchedFilter);

    /**
     * Called when an exported service is depublished. The concrete implementation is responsible for unregistering
     * the service in its service registry.
     * 
     * @param endpoint The service Endpoint Description
     * @param matchedFilter the matched filter
     */
    protected abstract void removePublishedEndpoint(EndpointDescription endpoint, String matchedFilter);

    /**
     * Called when an exported service is modified. The concrete implementation is responsible for updating
     * the service in its service registry.
     * 
     * @param endpoint The service Endpoint Description
     * @param matchedFilter The matched filter
     */
    protected abstract void modifyPublishedEndpoint(EndpointDescription endpoint, String matchedFilter);
}
