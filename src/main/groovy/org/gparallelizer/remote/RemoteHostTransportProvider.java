package org.gparallelizer.remote;

import java.util.*;

/**
 * Transport provider, which supports concept of remote hosts
 *
 * @author Alex Tkachman
 */
public abstract class RemoteHostTransportProvider extends RemoteTransportProvider {
    /**
     * Hosts known to the provider
     */
    protected final Map<UUID, RemoteHost> remoteHosts = new HashMap<UUID, RemoteHost>();

    /**
     * Local nodes known to the provider
     */
    protected final Map<UUID,LocalNode> localNodes = new HashMap<UUID,LocalNode> ();

    /**
     * Connect local node to the provider
     * @param node local node
     */
    @Override
    public void connect(LocalNode node) {
        super.connect (node);

        synchronized (localNodes) {
            localNodes.put(node.getId(), node);
        }

        synchronized (registry) {
            for (final RemoteNode n : registry.values()) {
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
    @Override
    public void disconnect(LocalNode node) {
        synchronized (remoteHosts) {
            for (final RemoteHost host : remoteHosts.values()) {
                host.disconnect(node);
            }
        }

        synchronized (registry) {
            for (final RemoteNode n : registry.values()) {
                if (!n.getId().equals(node.getId())) {
                    node.onDisconnect(n);
                }
            }
        }

        synchronized (localNodes) {
            localNodes.remove(node.getId());
        }

        super.disconnect(node);
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

        super.disconnect();
    }

    protected RemoteHostNode createNode(UUID uuid, RemoteHost remoteHost) {
        return new RemoteHostNode(uuid, remoteHost);
    }

    public RemoteHost getRemoteHost(UUID hostId, RemoteHostConnection connection) {
        synchronized (remoteHosts) {
            RemoteHost host = remoteHosts.get(hostId);
            if (host == null) {
                host = new RemoteHost(this);
                remoteHosts.put(hostId, host);
            }
            connection.setHost(host);
            host.addConnection(connection);
            return host;
        }
    }

    public void connectRemoteNode(UUID nodeId, RemoteHost host) {
        RemoteNode node;
        synchronized (registry) {
            node = registry.get(nodeId);
            if (node == null) {
                node = createNode(nodeId, host);
                registry.put(nodeId, node);
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
        synchronized (registry) {
            node = registry.remove(nodeId);
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
        synchronized (registry) {
            for (RemoteNode t : registry.values()) {
                if (((RemoteHostNode)t).getRemoteHost().getId().equals(host.getId())) {
                  toRemove.add(t);
                }
            }
            for (RemoteNode t : toRemove) {
                registry.remove(t.getId());
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
