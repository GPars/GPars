// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10, 2014  The original author or authors
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

package groovyx.gpars.remote.netty;

import groovyx.gpars.actor.Actor;
import groovyx.gpars.actor.remote.RemoteActorFuture;
import groovyx.gpars.dataflow.DataflowVariable;
import groovyx.gpars.dataflow.remote.RemoteDataflowBroadcast;
import groovyx.gpars.remote.BroadcastDiscovery;
import groovyx.gpars.remote.LocalHost;
import groovyx.gpars.remote.message.HostIdMsg;
import groovyx.gpars.remote.message.RemoteActorRequestMsg;
import groovyx.gpars.remote.message.RemoteDataflowReadChannelRequestMsg;
import groovyx.gpars.remote.message.RemoteDataflowVariableRequestMsg;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;


/**
 * Transport provider using Netty
 *
 * @author Alex Tkachman
 */
public class NettyTransportProvider {

    // final BroadcastDiscovery broadcastDiscovery;

    private static LocalHost localHost;

    private static NettyServer server;

    public static void startServer(String host, int port) {
        localHost = new LocalHost();
        server = new NettyServer(localHost, host, port, connection -> connection.write(new HostIdMsg(localHost.getId())));
        server.start();
    }

    public static void stopServer() {
        server.stop();
    }

    public static void stopClients() {
        localHost.disconnect();
    }

    public static void register(Actor actor, String name) {
        localHost.register(name, actor);
    }

    public static Future<Actor> get(String host, int port, String name) {
        if (localHost == null) {
            localHost = new LocalHost();
        }
        NettyClient client = new NettyClient(localHost, host, port, connection -> {
            if (connection.getHost() != null) {
                connection.write(new RemoteActorRequestMsg(localHost.getId(), name));
            }
        });
        client.start();

        DataflowVariable<Actor> remoteActor = new DataflowVariable<>();
        localHost.addRemoteActorFuture(name, remoteActor);
        return new RemoteActorFuture(remoteActor);
    }

    public static void getDataflowVariable(String host, int port, String name) {
        if (localHost == null) {
            localHost = new LocalHost();
        }
        NettyClient client = new NettyClient(localHost, host, port, connection -> {
            if (connection.getHost() != null)
                connection.write(new RemoteDataflowVariableRequestMsg(localHost.getId(), name));
        });
        client.start();
    }

    public static void setRemoteDataflowsRegistry(Map<String, DataflowVariable<?>> registry) {
        if (localHost == null) {
            localHost = new LocalHost();
        }
        if (localHost.getRemoteDataflowsRegistry() == null) {
            localHost.setRemoteDataflowsRegistry(registry);
        }
    }

    public static void getDataflowReadChannel(String host, int port, String name) {
        if (localHost == null) {
            localHost = new LocalHost();
        }

        NettyClient client = new NettyClient(localHost, host, port, connection -> {
            if (connection.getHost() != null)
                connection.write(new RemoteDataflowReadChannelRequestMsg(localHost.getId(), name));
        });
        client.start();
    }

    public static void setRemoteBroadcastsRegistry(Map<String, DataflowVariable<RemoteDataflowBroadcast>> remoteBroadcasts) {
        if (localHost == null) {
            localHost = new LocalHost();
        }

        if (localHost.getRemoteBroadcastsRegistry() == null) {
            localHost.setRemoteBroadcastsRegistry(remoteBroadcasts);
        }
    }
}
