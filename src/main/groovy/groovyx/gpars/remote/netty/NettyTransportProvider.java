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

import groovyx.gpars.remote.BroadcastDiscovery;
import groovyx.gpars.remote.LocalHost;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.UUID;


/**
 * Transport provider using Netty
 *
 * @author Alex Tkachman
 */
public class NettyTransportProvider extends LocalHost {

//    private final Map<UUID, Client> clients = new HashMap<UUID, Client>();

    final NettyServer server;

    final BroadcastDiscovery broadcastDiscovery;

    public NettyTransportProvider(String address) throws InterruptedException {
        server = new NettyServer(address);
        server.start();

        System.err.printf("Server listens on: %s:%d%n", server.getAddress().getHostString(), server.getAddress().getPort());

        this.broadcastDiscovery = new BroadcastDiscovery(getId(), server.getAddress()) {
            @Override
            protected void onDiscovery(final UUID uuid, final SocketAddress address) {
                System.err.println("discovered");
//                if (uuid.equals(getId())) {
//                    return;
//                }
//
//                synchronized (clients) {
//                    final Client client = clients.get(uuid);
//                    if (client == null) {
//                        clients.put(uuid, new Client(NettyTransportProvider.this, address, uuid));
//                    }
//                }
            }
        };

       broadcastDiscovery.start();
    }

    @Override
    public void disconnect() throws InterruptedException {
        super.disconnect();
        broadcastDiscovery.stop();

        server.stop();

//        for (final Client client : clients.values()) {
//            client.stop();
//        }
    }


//
//    @ChannelHandler.Sharable
//    public static class ClientHandler extends NettyHandler {
//        private final UUID id;
//
//        private final NettyTransportProvider provider;
//
//        public ClientHandler(final NettyTransportProvider provider, final UUID id) {
//            super(provider);
//            this.id = id;
//            this.provider = provider;
//        }
//
//        @Override
//        public void channelDisconnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
//            provider.clients.remove(id);
//            super.channelDisconnected(ctx, e);
//        }
//    }

}
