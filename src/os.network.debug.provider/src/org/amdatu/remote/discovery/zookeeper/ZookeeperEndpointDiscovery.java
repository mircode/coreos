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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.amdatu.remote.discovery.AbstractHttpEndpointDiscovery;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.osgi.service.remoteserviceadmin.EndpointDescription;

/**
 * Zookeeper implementation of service endpoint based discovery. This type of discovery discovers HTTP endpoints
 * that provide published services based on the {@link EndpointDescription} extender format.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class ZookeeperEndpointDiscovery extends AbstractHttpEndpointDiscovery<ZookeeperDiscoveryConfiguration>
    implements Watcher {

    public static final String DISCOVERY_NAME = "Amdatu Remote Service Endpoint (ZooKeeper)";
    public static final String DISCOVERY_TYPE = "zookeeper";

    private volatile List<URL> m_discoveryEndpointURLs = new ArrayList<URL>();
    private volatile ZooKeeper m_zooKeeper;
    private volatile boolean m_active;

    public ZookeeperEndpointDiscovery(ZookeeperDiscoveryConfiguration configuration) {
        super(DISCOVERY_TYPE, configuration);
    }

    @Override
    protected void startComponent() throws Exception {
        super.startComponent();

        m_zooKeeper =
            new ZooKeeper(getConfiguration().getConnectString(), getConfiguration().getTickTime(),
                ZookeeperEndpointDiscovery.this);

        String rootPath = getConfiguration().getRootPath();

        Stat rootStat = m_zooKeeper.exists(rootPath, false);
        if (rootStat == null) {
            logWarning("Creating Zookeeper discovery root znode: %s", rootPath);
            m_zooKeeper.create(rootPath, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }

        Stat nodeStat = m_zooKeeper.exists(getLocalNodePath(), false);
        if (nodeStat != null) {
            logWarning("Detected old znode. Pruging it: %s", getLocalNodePath());
            m_zooKeeper.delete(getLocalNodePath(), 0);
        }

        logInfo("Creating Zookeeper discovery node znode: %s", getLocalNodePath());
        m_zooKeeper.create(getLocalNodePath(), getConfiguration().getBaseUrl().toExternalForm().getBytes(),
            Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);

        m_active = true;
        List<String> children = m_zooKeeper.getChildren(rootPath, true);
        updateDiscoveryEndpoints(children);
    }

    @Override
    protected void stopComponent() throws Exception {
        m_active = false;

        try {
            m_zooKeeper.delete(getLocalNodePath(), 0);
        }
        catch (Exception e) {
            logWarning("Exception while stopping", e);
        }

        try {
            m_zooKeeper.close();
            Thread.sleep(100);
        }
        catch (Exception e) {
            logWarning("Exception while stopping", e);
        }
        m_zooKeeper = null;

        super.stopComponent();
    }

    @Override
    public void process(WatchedEvent event) {
        if (m_active && event.getType() == EventType.NodeChildrenChanged) {
            try {
                List<String> children = m_zooKeeper.getChildren(getConfiguration().getRootPath(), true);
                updateDiscoveryEndpoints(children);
            }
            catch (Exception e) {
                logWarning("Failed to update discovery endpoints", e);
            }
        }
    }

    private String getLocalNodePath() {
        return getNodePath(getFrameworkUUID());
    }

    private String getNodePath(String nodeID) {
        String rootPath = getConfiguration().getRootPath();
        if (rootPath.endsWith("/")) {
            return rootPath + nodeID;
        }
        return rootPath + "/" + nodeID;
    }

    private void updateDiscoveryEndpoints(List<String> discoveryEndpointIDs) throws Exception {

        discoveryEndpointIDs.remove(getFrameworkUUID());

        List<URL> discoveryEndpointURLs = new ArrayList<URL>();
        for (String discoveryEndpointID : discoveryEndpointIDs) {
            try {
                String nodePath = getNodePath(discoveryEndpointID);
                byte[] bytes = m_zooKeeper.getData(nodePath, false, null);
                discoveryEndpointURLs.add(new URL(new String(bytes)));
            }
            catch (Exception e) {
                logWarning("Failed to retrieve  discovery endpoint URL for id: %s", e, discoveryEndpointID);
            }
        }

        synchronized (m_discoveryEndpointURLs) {
            for (URL discoveryEndpointURL : m_discoveryEndpointURLs) {
                if (!discoveryEndpointURLs.contains(discoveryEndpointURL)) {
                    removeDiscoveryEndpoint(discoveryEndpointURL);
                }
            }

            for (URL discoveryEndpointURL : discoveryEndpointURLs) {
                if (!m_discoveryEndpointURLs.contains(discoveryEndpointURL)) {
                    addDiscoveryEndpoint(discoveryEndpointURL);
                }
            }
            m_discoveryEndpointURLs = discoveryEndpointURLs;
        }
    }
}
