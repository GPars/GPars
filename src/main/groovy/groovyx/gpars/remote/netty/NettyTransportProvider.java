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
import groovyx.gpars.remote.BroadcastDiscovery;
import groovyx.gpars.remote.LocalHost;
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

    private final static LocalHost localHost = new LocalHost();

    private static NettyServer server;

    // final BroadcastDiscovery broadcastDiscovery;

    public static void register(Actor actor, String name) {
        if (server == null) {
            startServer();
        }
        localHost.register(name, actor);
    }

    public static Future<Actor> get(String host, int port, String name) {
        NettyClient client = new NettyClient(localHost, host, port);
        client.start();

        RemoteActorFuture future = new RemoteActorFuture(name);
        return future;
    }

    private static void startServer() {
        server = new NettyServer(localHost, "localhost", 9000);
        server.start();
    }
}
