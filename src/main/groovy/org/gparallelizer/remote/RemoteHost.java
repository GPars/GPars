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

import org.gparallelizer.remote.messages.*;
import org.gparallelizer.remote.serial.*;
import org.gparallelizer.actors.ActorMessage;
import org.gparallelizer.actors.ReplyRegistry;
import org.gparallelizer.MessageStream;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.io.ObjectStreamException;
import java.io.InvalidObjectException;

/**
 * Representation of remote host connected to transport provider
 * 
 * @author Alex Tkachman
 */
public final class RemoteHost  {
    private final RemoteTransportProvider provider;
    private final UUID hostId;

    private final ArrayList<RemoteConnection> connections = new ArrayList<RemoteConnection> ();

    private static final ThreadLocal<RemoteHost> threadContext = new ThreadLocal<RemoteHost>();

    public RemoteHost(RemoteTransportProvider provider, UUID hostId) {
        this.provider = provider;
        this.hostId = hostId;
    }

    public void addConnection(RemoteConnection connection) {
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

    public void removeConnection(RemoteConnection connection) {
        synchronized (connections) {
            boolean wasConnected = isConnected();
            connections.remove(connection);
            if (wasConnected != isConnected()) {
//            sendLocalNodes();
            }
        }
    }

    public void disconnect() {
        for (RemoteConnection connection : connections) {
            connection.disconnect ();
        }
    }

    public boolean isConnected() {
        return connections.size() != 0;
    }

    public void write(BaseMsg msg) {
        getConnection().write(msg);
    }

    public RemoteConnection getConnection() {
        return connections.get(0);
    }

    public UUID getId() {
        return provider.getId();
    }

    public void onMessage(BaseMsg msg) {
        if (msg instanceof NodeConnectedMsg) {
            NodeConnectedMsg connectedMsg = (NodeConnectedMsg) msg;
            provider.connectRemoteNode(connectedMsg.nodeId, this, connectedMsg.mainActor);
            return;
        }

        if (msg instanceof NodeDisconnectedMsg) {
            NodeDisconnectedMsg connectedMsg = (NodeDisconnectedMsg) msg;
            provider.disconnectRemoteNode(connectedMsg.nodeId);
            return;
        }

        if (msg instanceof MessageToActor) {
            MessageToActor rm = (MessageToActor) msg;
            rm.getTo().send(rm.getMessage());
        }

        if (msg instanceof StopActorMsg) {
            StopActorMsg sm = (StopActorMsg) msg;
            sm.actor.stop();
        }
    }

    public void connect(LocalNode node) {
        write(new NodeConnectedMsg(node, getHostId()));
    }

    public void disconnect(LocalNode node) {
        write(new NodeDisconnectedMsg(node, getHostId()));
    }

    public RemoteTransportProvider getProvider() {
        return provider;
    }

    public void stop(RemoteActor remoteActor) {
        write(new StopActorMsg(remoteActor, getHostId()));
    }

    public final void send(MessageStream receiver, Object message) {
        if (!(message instanceof ActorMessage)) {
            message = new ActorMessage<Object> (message, ReplyRegistry.threadBoundActor());
        }
        write(new MessageToActor(getHostId(), receiver, (ActorMessage)message));
    }

    public UUID getHostId() {
        return hostId;
    }

    public static RemoteHost getThreadContext() {
        return threadContext.get();
    }

    public void enter() {
        threadContext.set(this);
    }

    public void leave() {
        threadContext.set(null);
    }

    public WithSerialId readResolve(Object handle) throws ObjectStreamException {
        if (handle == null)
            return null;

        return provider.readResolve(handle);
    }

    public Object writeReplace(WithSerialId object) throws ObjectStreamException {
        if (object == null)
            return null;

        if (object instanceof RemoteSerialized) {
            return new LocalHandle(object.serialId);
        }

        return provider.writeReplace (object);
    }
}
