package org.gparallelizer.remote;

import org.gparallelizer.remote.messages.BaseMsg;
import org.gparallelizer.remote.messages.MessageToActor;
import org.gparallelizer.remote.messages.NodeConnectedMsg;
import org.gparallelizer.remote.messages.NodeDisconnectedMsg;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

/**
 * Representation of remote host connected to transport provider
 * 
 * @author Alex Tkachman
 */
public class RemoteHost {
    private final RemoteHostTransportProvider provider;

    private final ArrayList<RemoteHostConnection> connections = new ArrayList<RemoteHostConnection> ();

    public RemoteHost(RemoteHostTransportProvider provider) {
        this.provider = provider;
    }

    public void addConnection(RemoteHostConnection connection) {
        synchronized (connections) {
            boolean wasConnected = isConnected();
            connections.add(connection);
            if (wasConnected != isConnected()) {
                Map<UUID,LocalNode> localNodes = provider.localNodes;
                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (localNodes) {
                    for (LocalNode localNode : localNodes.values()) {
                        connection.write(new NodeConnectedMsg(localNode, provider.getId()));
                    }
                }
            }
        }
    }

    public void removeConnection(RemoteHostConnection connection) {
        synchronized (connections) {
            boolean wasConnected = isConnected();
            connections.remove(connection);
            if (wasConnected != isConnected()) {
//            sendLocalNodes();
            }
        }
    }

    public void disconnect() {
        for (RemoteHostConnection connection : connections) {
            connection.disconnect ();
        }
    }

    public boolean isConnected() {
        return connections.size() != 0;
    }

    public void send(BaseMsg msg) {
        getConnection().write(msg);
    }

    public RemoteHostConnection getConnection() {
        return connections.get(0);
    }

    public UUID getId() {
        return provider.getId();
    }

    public void onMessage(BaseMsg msg) {
        if (msg instanceof NodeConnectedMsg) {
            NodeConnectedMsg connectedMsg = (NodeConnectedMsg) msg;
            provider.connectRemoteNode(connectedMsg.nodeId, this);
            return;
        }

        if (msg instanceof NodeDisconnectedMsg) {
            NodeDisconnectedMsg connectedMsg = (NodeDisconnectedMsg) msg;
            provider.disconnectRemoteNode(connectedMsg.nodeId);
            return;
        }

        if (msg instanceof MessageToActor) {
            MessageToActor rm = (MessageToActor) msg;
            provider.getLocalActor(rm.getTo()).send(rm.getPayload());
        }
    }

    public void connect(LocalNode node) {
        send(new NodeConnectedMsg(node, getId()));
    }

    public void disconnect(LocalNode node) {
        send(new NodeDisconnectedMsg(node, getId()));
    }

    public RemoteTransportProvider getProvider() {
        return provider;
    }
}