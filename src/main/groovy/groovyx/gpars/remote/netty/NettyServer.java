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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

/**
 * Represents a server that waits for connections from clients.
 * @see NettyClient
 */
public class NettyServer {
    private final String address;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel channel;

    /**
     * Creates a server listening on specified addresss.
     * @param address
     */
    public NettyServer(String address) {
        this.address = address;
    }

    /**
     * Starts the server.
     * Note: method blocks until server is started.
     * @throws InterruptedException
     */
    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(new NettyChannelInitializer())
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .localAddress(new InetSocketAddress(address, 0));

        channel = bootstrap.bind().sync().channel();
    }

    /**
     * Stops the server.
     * Note: method block untils server is stopped.
     * @throws InterruptedException
     */
    public void stop() throws InterruptedException {
        channel.close().sync();

        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    public InetSocketAddress getAddress() {
        return (InetSocketAddress) channel.localAddress();
    }
}
