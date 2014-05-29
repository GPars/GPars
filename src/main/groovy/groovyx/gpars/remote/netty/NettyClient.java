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

import groovyx.gpars.remote.BroadcastDiscovery;
import groovyx.gpars.remote.LocalHost;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents client that connects to server
 * @see NettyServer
 */
public class NettyClient {
    private final String host;
    private final int port;
    private EventLoopGroup workerGroup;

    private Channel channel;

    private List<DisconnectListener> disconnectListeners = new ArrayList<>();

    private LocalHost localHost;

    /**
     * Creates client that connect to server on specified host and port.
     * @param host the host where server listens on
     * @param port the port that server listens on
     */
    public NettyClient(LocalHost localHost, String host, int port) {
        this.localHost = localHost;
        this.host = host;
        this.port = port;
    }

    /**
     * Connects the client to server
     * Note: method block until connection is established
     * @throws InterruptedException
     */
    public void start() throws InterruptedException {
        workerGroup = new NioEventLoopGroup();

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .handler(new NettyChannelInitializer(localHost, disconnectListeners))
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .remoteAddress(host, port);

        channel = bootstrap.connect().sync().channel();
    }

    /**
     * Closes connection to server
     * Note: method blocks until connection is closed
     * @throws InterruptedException
     */
    public void stop() throws InterruptedException {
        channel.close().sync();
        workerGroup.shutdownGracefully();
    }

    public void addDisconnectListener(DisconnectListener listener) {
        disconnectListeners.add(listener);
    }

    @FunctionalInterface
    public interface DisconnectListener {
        public void onDisconnect() throws InterruptedException;
    }
}
