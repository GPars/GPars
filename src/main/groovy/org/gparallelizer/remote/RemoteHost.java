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

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

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
                        connection.write(new NodeConnectedMsg(localNode));
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

    public void write(AbstractMsg msg) {
        msg.hostId = getProvider().getId();
        getConnection().write(msg);
    }

    public RemoteConnection getConnection() {
        return connections.get(0);
    }

    public UUID getId() {
        return provider.getId();
    }

    public void connect(LocalNode node) {
        write(new NodeConnectedMsg(node));
    }

    public void disconnect(LocalNode node) {
        write(new NodeDisconnectedMsg(node));
    }

    public RemoteTransportProvider getProvider() {
        return provider;
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

}
