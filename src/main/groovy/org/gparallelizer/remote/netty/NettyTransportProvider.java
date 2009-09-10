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

package org.gparallelizer.remote.netty;

import org.gparallelizer.remote.BroadcastDiscovery;
import org.gparallelizer.remote.RemoteHostConnection;
import org.gparallelizer.remote.RemoteHostTransportProvider;
import org.gparallelizer.remote.messages.BaseMsg;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

/**
 * Transport provider using Netty
 * 
 * @author Alex Tkachman
 */
public class NettyTransportProvider extends RemoteHostTransportProvider {

    private final Map<UUID,Client> clients = new HashMap<UUID,Client>();

    public NettyTransportProvider() {
        Server server = new Server(this);
        new BroadcastDiscovery(getId(), server.getAddress()) {
            @Override
            protected void onDiscovery(UUID uuid, SocketAddress address) {
                if (uuid.equals(getId()))
                    return;
                
                synchronized (clients) {
                    Client client = clients.get(uuid);
                    if (client == null) {
                        clients.put(uuid, new Client(NettyTransportProvider.this, address, uuid));
                    }
                }
            }
        };
    }

    public static class Server {
        private InetSocketAddress address;

        public Server(NettyTransportProvider provider) {
            ChannelFactory factory =
                new NioServerSocketChannelFactory(
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool());

            ServerBootstrap bootstrap = new ServerBootstrap(factory);

            bootstrap.setPipelineFactory(new ServerPipelineFactory(provider));
            bootstrap.setOption("child.tcpNoDelay", true);
            bootstrap.setOption("child.keepAlive", true);

            Channel channel = bootstrap.bind(new InetSocketAddress(0));
            address = ((InetSocketAddress)channel.getLocalAddress());
        }

        public InetSocketAddress getAddress() {
            return address;
        }
    }

    public static class Client {
        private final NettyTransportProvider provider;

        public Client(NettyTransportProvider provider, SocketAddress address, UUID id) {
            this.provider = provider;
            final ChannelFactory factory =
                new NioClientSocketChannelFactory(
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool());

            ClientBootstrap bootstrap = new ClientBootstrap(factory);

            Handler handler = new ClientHandler(this.provider, id);

            bootstrap.getPipeline().addLast("handler", handler);
            bootstrap.setOption("tcpNoDelay", true);
            bootstrap.setOption("keepAlive", true);

            bootstrap.connect(address);
        }
    }

    public static class ServerPipelineFactory implements ChannelPipelineFactory {
        private final NettyTransportProvider provider;

        public ServerPipelineFactory(NettyTransportProvider provider) {
            this.provider = provider;
        }

        public ChannelPipeline getPipeline() throws Exception {
            ChannelPipeline pipeline = org.jboss.netty.channel.Channels.pipeline();
            pipeline.addLast("handler", new Handler(provider));
            return pipeline;
        }
    }

    @ChannelPipelineCoverage("one")
    public static class Handler extends SimpleChannelHandler {

        private Channel channel;

        private final RemoteHostConnection connection;

        public Handler(NettyTransportProvider provider) {
            connection = new NettyRemoteConnection(provider, this);
        }

        @Override
        public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            channel = e.getChannel();
            channel.getPipeline().addFirst("encoder", new ObjectEncoder());
            channel.getPipeline().addFirst("decoder", new ObjectDecoder());
        }

        @Override
        public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            connection.onConnect();
        }

        @Override
        public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            connection.onDisconnect();
        }

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
            connection.onMessage((BaseMsg) e.getMessage());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            connection.onException(e.getCause());
        }

        public Channel getChannel() {
            return channel;
        }
    }

    @ChannelPipelineCoverage("one")
    public static class ClientHandler extends Handler {
        private UUID id;

        private NettyTransportProvider provider;

        public ClientHandler(NettyTransportProvider provider, UUID id) {
            super(provider);
            this.id = id;
            this.provider = provider;
        }

        @Override
        public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            provider.clients.remove(id);
            super.channelDisconnected(ctx, e);
        }
    }
}
