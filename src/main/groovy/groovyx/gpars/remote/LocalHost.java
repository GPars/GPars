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
import groovyx.gpars.actor.remote.RemoteActorFuture;
import groovyx.gpars.dataflow.DataflowQueue;
import groovyx.gpars.dataflow.DataflowVariable;
import groovyx.gpars.dataflow.remote.RemoteDataflowBroadcast;
import groovyx.gpars.dataflow.remote.RemoteDataflowQueue;
import groovyx.gpars.dataflow.remote.RemoteDataflowVariable;
import groovyx.gpars.remote.netty.NettyClient;
import groovyx.gpars.remote.netty.NettyServer;
import groovyx.gpars.remote.netty.NettyTransportProvider;
import groovyx.gpars.serial.SerialContext;
import groovyx.gpars.serial.SerialHandles;
import groovyx.gpars.serial.SerialMsg;

import java.util.*;

/**
 * Represents communication point with other local hosts.
 * Usually it is enough to have one LocalHost per JVM but it is possible to have several.
 * <p>
 * It can be one or several local nodes hosted on local host. For most applications one should be enough
 * but sometimes several can be useful as well.
 * </p>
 * <p>
 * Local host contains: remote hosts connected with this one
 * </p>
 *
 * @author Alex Tkachman
 */
public abstract class LocalHost extends SerialHandles {
    /**
     * Hosts known to the provider
     */
    protected final Map<UUID, RemoteHost> remoteHosts = new HashMap<UUID, RemoteHost>();

    /**
     * Server for current instance of LocalHost
     */
    private NettyServer server;

    public void disconnect() {
        synchronized (remoteHosts) {
            final Iterable<RemoteHost> copy = new ArrayList<RemoteHost>(remoteHosts.values());
            remoteHosts.clear();
            for (final RemoteHost remoteHost : copy) {
                remoteHost.disconnect();
            }
        }
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
//        RemoteNode node;
//        synchronized (remoteNodes) {
//            node = remoteNodes.get(nodeId);
//            if (node == null) {
//                node = new RemoteNode(nodeId, host, mainActor);
//                remoteNodes.put(nodeId, node);
//            }
//        }
//
//        synchronized (localNodes) {
//            for (final LocalNode localNode : localNodes.values()) {
//                localNode.onConnect(node);
//            }
//        }
    }

    public void disconnectRemoteNode(final UUID nodeId) {
//        final RemoteNode node;
//        synchronized (remoteNodes) {
//            node = remoteNodes.remove(nodeId);
//        }
//
//        if (node != null) {
//            synchronized (localNodes) {
//                onDisconnectForLocalNodes(node);
//            }
//        }
    }

    public void onDisconnect(final SerialContext host) {
//        final Collection<RemoteNode> toRemove = new ArrayList<RemoteNode>();
//        synchronized (remoteNodes) {
//            for (final RemoteNode t : remoteNodes.values()) {
//                if (t.getRemoteHost() == host) {
//                    toRemove.add(t);
//                }
//            }
//            for (final RemoteNode t : toRemove) {
//                remoteNodes.remove(t.getId());
//            }
//        }
//
//        synchronized (localNodes) {  //todo consider moving the synchronized block inside the onDisconnectForLocalNodes() method
//            for (final RemoteNode t : toRemove) {
//                onDisconnectForLocalNodes(t);
//            }
//        }
    }

    public abstract <T> void registerProxy(Class<T> klass, String name, T object);

    public abstract <T> T get(Class<T> klass, String name);

    public void startServer(String host, int port) {
        if (server != null) {
            throw new IllegalStateException("Server is already started");
        }

        server = NettyTransportProvider.createServer(host, port, this);
        server.start();
    }

    public void stopServer() {
        if (server == null) {
            throw new IllegalStateException("Server has not been started");
        }

        server.stop();
    }

    private void createRequest(String host, int port, SerialMsg msg) {
        NettyClient client = NettyTransportProvider.createClient(host, port, this, connection -> {
            if (connection.getHost() != null)
                connection.write(msg);
        });
        client.start();
    }

    protected <T> DataflowVariable<T> getPromise(Map<String, DataflowVariable<T>> registry, String name, String host, int port, SerialMsg requestMsg) {
        DataflowVariable remoteVariable = registry.get(name);
        if (remoteVariable == null) {
            DataflowVariable newRemoteVariable = new DataflowVariable<>();
            remoteVariable = registry.putIfAbsent(name, newRemoteVariable);
            if (remoteVariable == null) {
                createRequest(host, port, requestMsg);
                remoteVariable = newRemoteVariable;
            }

        }
        return remoteVariable;
    }
}
