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

import groovyx.gpars.remote.LocalHost;


/**
 * Transport provider using Netty
 *
 * @author Alex Tkachman
 */
public class NettyTransportProvider extends LocalHost {

//    private final Map<UUID, Client> clients = new HashMap<UUID, Client>();

    final NettyServer server = new NettyServer("localhost");

//    final BroadcastDiscovery broadcastDiscovery;

    public NettyTransportProvider() {
//        server.start(this);
//        System.err.println(server.getAddress().getAddress().getHostAddress() + ":" + server.getAddress().getPort());

//        this.broadcastDiscovery = new BroadcastDiscovery(getId(), server.getAddress()) {
//            @Override
//            protected void onDiscovery(final UUID uuid, final SocketAddress address) {
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
//            }
//        };

//        broadcastDiscovery.start();
    }

    @Override
    public void disconnect() {
//        broadcastDiscovery.stop();

        super.disconnect();

//        server.stop();

//        for (final Client client : clients.values()) {
//            client.stop();
//        }
    }

//    public static class Client {
//        private final NettyTransportProvider provider;
//
//        final ChannelFuture channelFuture;
//
//        final ChannelFactory factory;
//
//        @SuppressWarnings({"UnnecessaryBoxing"})
//        public Client(final NettyTransportProvider provider, final SocketAddress address, final UUID id) {
//            this.provider = provider;
//            factory = new NioClientSocketChannelFactory(
//                    Executors.newCachedThreadPool(MyThreadFactory.instance),
//                    Executors.newCachedThreadPool(MyThreadFactory.instance));
//
//            final ClientBootstrap bootstrap = new ClientBootstrap(factory);
//
//            final NettyHandler handler = new ClientHandler(this.provider, id);
//
//            bootstrap.getPipeline().addLast("handler", handler);
//            bootstrap.setOption("tcpNoDelay", Boolean.valueOf(true));
//            bootstrap.setOption("keepAlive", Boolean.valueOf(true));
//
//            channelFuture = bootstrap.connect(address);
//        }
//
//        public void stop() {
//            channelFuture.getChannel().close().addListener(new ChannelFutureListener() {
//                @Override
//                public void operationComplete(final ChannelFuture future) throws Exception {
//                    factory.releaseExternalResources();
//                }
//            });
//        }
//    }

//    public static class ServerPipelineFactory implements ChannelPipelineFactory {
//        private final NettyTransportProvider provider;
//
//        public ServerPipelineFactory(final NettyTransportProvider provider) {
//            this.provider = provider;
//        }
//
//        @Override
//        public ChannelPipeline getPipeline() throws Exception {
//            final ChannelPipeline pipeline = Channels.pipeline();
//            pipeline.addLast("handler", new NettyHandler(provider));
//            return pipeline;
//        }
//    }
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
//
//    private static class MyThreadFactory implements ThreadFactory {
//        static final MyThreadFactory instance = new MyThreadFactory();
//
//        @Override
//        public Thread newThread(final Runnable r) {
//            final Thread thread = new Thread(r);
//            thread.setDaemon(true);
//            thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
//                @Override
//                public void uncaughtException(final Thread t, final Throwable e) {
//                    e.printStackTrace();
//                }
//            });
//            return thread;
//        }
//    }
}
