// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2010, 2013  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.remote;

import groovyx.gpars.actor.Actor;
import groovyx.gpars.serial.SerialContext;
import groovyx.gpars.serial.SerialHandles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents communication point with other local hosts.
 * Usually it is enough to have one LocalHost per JVM but it is possible to have several.
 * <p>
 * It can be one or several local nodes hosted on local host. For most applications one should be enough
 * but sometimes several can be useful as well.
 * </p>
 * <p>
 * Local host contains
 * <ul>
 *   <li>remote hosts connected with this one</li>
 *   <li>remote nodes known to this host</li>
 *   <li>local nodes available on this host</li>
 * </ul>
 * </p>
 *
 * @author Alex Tkachman
 */
public class LocalHost extends SerialHandles {

    /**
     * Registry of remote nodes known to the provider
     */
    protected final HashMap<UUID, RemoteNode> remoteNodes = new HashMap<UUID, RemoteNode>();

    /**
     * Hosts known to the provider
     */
    protected final Map<UUID, RemoteHost> remoteHosts = new HashMap<UUID, RemoteHost>();

    /**
     * Local nodes known to the provider
     */
    protected final Map<UUID, LocalNode> localNodes = new HashMap<UUID, LocalNode>();

    /**
     * Connect local node to the provider
     *
     * @param node local node
     */
    public void connect(final LocalNode node) {
        synchronized (localNodes) {
            localNodes.put(node.getId(), node);
        }

        synchronized (remoteNodes) {
            for (final RemoteNode remoteNode : remoteNodes.values()) {
                if (!remoteNode.getId().equals(node.getId())) {
                    node.onConnect(remoteNode);
                }
            }
        }

        synchronized (remoteHosts) {
            for (final RemoteHost host : remoteHosts.values()) {
                host.connect(node);
            }
        }
    }

    /**
     * Disconnect local node from the provider
     *
     * @param node local node
     */
    public void disconnect(final LocalNode node) {
        synchronized (remoteHosts) {
            for (final RemoteHost host : remoteHosts.values()) {
                host.disconnect(node);
            }
        }

        synchronized (remoteNodes) {
            for (final RemoteNode remoteNode : remoteNodes.values()) {
                if (!remoteNode.getId().equals(node.getId())) {
                    node.onDisconnect(remoteNode);
                }
            }
        }

        synchronized (localNodes) {
            localNodes.remove(node.getId());
        }
    }

    public void disconnect() {
        synchronized (localNodes) {
            final Iterable<LocalNode> copy = new ArrayList<LocalNode>(localNodes.values());
            localNodes.clear();
            for (final LocalNode localNode : copy) {
                disconnect(localNode);
            }
        }

        synchronized (remoteHosts) {
            final Iterable<RemoteHost> copy = new ArrayList<RemoteHost>(remoteHosts.values());
            remoteHosts.clear();
            for (final RemoteHost remoteHost : copy) {
                remoteHost.disconnect();
            }
        }

        LocalHostRegistry.removeLocalHost(this);
    }

    @Override
    public SerialContext getSerialHost(final UUID hostId, final Object attachment) {
        final RemoteConnection connection = (RemoteConnection) attachment;
        synchronized (remoteHosts) {
            RemoteHost host = remoteHosts.get(hostId);
            if (host == null) {
                host = new RemoteHost(this, hostId);
                remoteHosts.put(hostId, host);
            }
            if (connection != null) {
                connection.setHost(host);
                host.addConnection(connection);
            }
            return host;
        }
    }

    public void connectRemoteNode(final UUID nodeId, final SerialContext host, final Actor mainActor) {
        RemoteNode node;
        synchronized (remoteNodes) {
            node = remoteNodes.get(nodeId);
            if (node == null) {
                node = new RemoteNode(nodeId, host, mainActor);
                remoteNodes.put(nodeId, node);
            }
        }

        synchronized (localNodes) {
            for (final LocalNode localNode : localNodes.values()) {
                localNode.onConnect(node);
            }
        }
    }

    public void disconnectRemoteNode(final UUID nodeId) {
        final RemoteNode node;
        synchronized (remoteNodes) {
            node = remoteNodes.remove(nodeId);
        }

        if (node != null) {
            synchronized (localNodes) {
                onDisconnectForLocalNodes(node);
            }
        }
    }

    public void onDisconnect(final SerialContext host) {
        final Collection<RemoteNode> toRemove = new ArrayList<RemoteNode>();
        synchronized (remoteNodes) {
            for (final RemoteNode t : remoteNodes.values()) {
                if (t.getRemoteHost() == host) {
                    toRemove.add(t);
                }
            }
            for (final RemoteNode t : toRemove) {
                remoteNodes.remove(t.getId());
            }
        }

        synchronized (localNodes) {  //todo consider moving the synchronized block inside the onDisconnectForLocalNodes() method
            for (final RemoteNode t : toRemove) {
                onDisconnectForLocalNodes(t);
            }
        }
    }

    private void onDisconnectForLocalNodes(final RemoteNode t) {
        for (final LocalNode localNode : localNodes.values()) {
            localNode.onDisconnect(t);
        }
    }
}
