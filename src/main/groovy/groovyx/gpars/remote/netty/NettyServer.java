// GPars - Groovy Parallel Systems
//
// Copyright © 2014  The original author or authors
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
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.ConnectException;
import java.net.InetSocketAddress;

/**
 * Represents a server that waits for connections from clients.
 * @see NettyClient
 */
public class NettyServer {
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private ServerBootstrap bootstrap;

    private LocalHost localHost;
    private ChannelFuture channelFuture;

    /**
     * Creates a server listening on specified addresss.
     * @param address
     */
    public NettyServer(LocalHost localHost, String address, int port) {
        this.localHost = localHost;

        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();

        bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new NettyChannelInitializer(localHost))
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .localAddress(new InetSocketAddress(address, port));
    }

    /**
     * Starts the server.
     * Note: this method does not block
     */
    public void start() throws InterruptedException {
        if (channelFuture == null) {
            channelFuture = bootstrap.bind().addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        }
    }

    /**
     * Stops the server.
     * Note: method block until server is stopped.
     * @throws InterruptedException
     */
    public void stop() throws InterruptedException {
        channelFuture.channel().close().sync();

        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    public InetSocketAddress getAddress() {
        return (InetSocketAddress) channelFuture.channel().localAddress();
    }
}
