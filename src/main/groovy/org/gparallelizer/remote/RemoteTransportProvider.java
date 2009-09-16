//  GParallelizer
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License. 

package org.gparallelizer.remote;

import org.gparallelizer.actors.Actor;
import org.gparallelizer.remote.serial.*;

import java.util.*;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.WriteAbortedException;

/**
 * Represents communication method with remote hosts
 *
 * @author Alex Tkachman
 */
public abstract class RemoteTransportProvider {

    /**
     * Unique id of the provider
     */
    private final UUID id = UUID.randomUUID();

    /**
     * Registry of remote nodes known to the provider
     */
    protected final HashMap<UUID, RemoteNode> nodeRegistry = new HashMap<UUID, RemoteNode>();

    /**
     * Hosts known to the provider
     */
    protected final Map<UUID, RemoteHost> remoteHosts = new HashMap<UUID, RemoteHost>();
    /**
     * Local nodes known to the provider
     */
    protected final Map<UUID,LocalNode> localNodes = new HashMap<UUID,LocalNode> ();

    public final HashMap<UUID, SerialHandle> localHandles  = new HashMap<UUID, SerialHandle> ();

    private final HashMap<UUID, SerialHandle> remoteHandles  = new HashMap<UUID, SerialHandle> ();

    /**
     * Getter for provider id
     *
     * @return unique id
     */
    public UUID getId() {
        return id;
    }

    public void finalizeHandle(SerialHandle handle) {
        localHandles.remove(handle.getSerialId());
    }

    /**
     * Connect local node to the provider
     * @param node local node
     */
    public void connect(LocalNode node) {
        synchronized (localNodes) {
            localNodes.put(node.getId(), node);
        }

        synchronized (nodeRegistry) {
            for (final RemoteNode n : nodeRegistry.values()) {
                if (!n.getId().equals(node.getId())) {
                    node.onConnect(n);
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
    public void disconnect(LocalNode node) {
        synchronized (remoteHosts) {
            for (final RemoteHost host : remoteHosts.values()) {
                host.disconnect(node);
            }
        }

        synchronized (nodeRegistry) {
            for (final RemoteNode n : nodeRegistry.values()) {
                if (!n.getId().equals(node.getId())) {
                    node.onDisconnect(n);
                }
            }
        }

        synchronized (localNodes) {
            localNodes.remove(node.getId());
        }
    }

    public void disconnect () {
        synchronized (localNodes) {
            ArrayList<LocalNode> copy = new ArrayList<LocalNode>(localNodes.values());
            localNodes.clear();
            for (LocalNode localNode : copy) {
                disconnect(localNode);
            }
        }

        synchronized (remoteHosts) {
            ArrayList<RemoteHost> copy = new ArrayList<RemoteHost>(remoteHosts.values());
            remoteHosts.clear();
            for (RemoteHost remoteHost : copy) {
                remoteHost.disconnect();
            }
        }

        TransportRegistry.removeTransportProvider(this);
    }

    public RemoteHost getRemoteHost(UUID hostId, RemoteConnection connection) {
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

    public void connectRemoteNode(UUID nodeId, RemoteHost host, Actor mainActor) {
        RemoteNode node;
        synchronized (nodeRegistry) {
            node = nodeRegistry.get(nodeId);
            if (node == null) {
                node = new RemoteNode(nodeId, host, mainActor);
                nodeRegistry.put(nodeId, node);
            }
        }

        synchronized (localNodes) {
            for (LocalNode localNode : localNodes.values()) {
                localNode.onConnect(node);
            }
        }
    }

    public void disconnectRemoteNode(UUID nodeId) {
        RemoteNode node;
        synchronized (nodeRegistry) {
            node = nodeRegistry.remove(nodeId);
        }

        if (node != null)
            synchronized (localNodes) {
                for (LocalNode localNode : localNodes.values()) {
                    localNode.onDisconnect(node);
                }
            }
    }

    public void onDisconnect(RemoteHost host) {
        ArrayList<RemoteNode> toRemove = new ArrayList<RemoteNode> ();
        synchronized (nodeRegistry) {
            for (RemoteNode t : nodeRegistry.values()) {
                if (t.getRemoteHost() == host) {
                  toRemove.add(t);
                }
            }
            for (RemoteNode t : toRemove) {
                nodeRegistry.remove(t.getId());
            }
        }

        synchronized (localNodes) {
            for (RemoteNode t : toRemove) {
                for (LocalNode localNode : localNodes.values()) {
                    localNode.onDisconnect(t);
                }
            }
        }
    }

}
