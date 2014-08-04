// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
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
import groovyx.gpars.remote.message.CloseConnectionMsg;
import groovyx.gpars.remote.message.NodeConnectedMsg;
import groovyx.gpars.remote.message.NodeDisconnectedMsg;
import groovyx.gpars.serial.SerialContext;
import groovyx.gpars.serial.SerialMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Representation of remote host connected to transport provider
 *
 * @author Alex Tkachman
 */
public final class RemoteHost extends SerialContext {
    private final ArrayList<RemoteConnection> connections = new ArrayList<>();

    public RemoteHost(final LocalHost localHost, final UUID hostId) {
        super(localHost, hostId);
    }

    public void addConnection(final RemoteConnection connection) {
        synchronized (connections) {
            final boolean wasConnected = isConnected();
            connections.add(connection);
            if (wasConnected != isConnected()) {
//                final Map<UUID, LocalNode> localNodes = ((LocalHost) localHost).localNodes;
//                //noinspection SynchronizationOnLocalVariableOrMethodParameter
//                synchronized (localNodes) {
//                    for (final LocalNode localNode : localNodes.values()) {
//                        connection.write(new NodeConnectedMsg(localNode));
//                    }
//                }
//                final List<Actor> localActors = ((LocalHost) localHost).localActors;
//                synchronized (localActors) {
//                    localActors.stream().forEach((actor) -> connection.write(new NodeConnectedMsg(actor)));
//                }
            }
        }
    }

    public void removeConnection(final RemoteConnection connection) {
        synchronized (connections) {
            final boolean wasConnected = isConnected();
            connections.remove(connection);
            if (wasConnected != isConnected()) {
//            sendLocalNodes();
            }
        }
    }

    public void disconnect() {
        for (final RemoteConnection connection : connections) {
            // connection.disconnect();
            connection.write(new CloseConnectionMsg());
        }
    }

    public boolean isConnected() {
        return !connections.isEmpty();
    }

    @Override
    public void write(final SerialMsg msg) {
        msg.hostId = getLocalHost().getId();
        getConnection().write(msg);
    }

    public RemoteConnection getConnection() {
        return connections.get(0);
    }

//    public void connect(final LocalNode node) {
//        write(new NodeConnectedMsg(node));
//    }
//
//    public void disconnect(final LocalNode node) {
//        write(new NodeDisconnectedMsg(node));
//    }

    public LocalHost getLocalHost() {
        return (LocalHost) localHost;
    }
}
