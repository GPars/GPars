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
import groovyx.gpars.remote.RemoteConnection;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

import java.util.ArrayList;
import java.util.List;

public class NettyChannelInitializer extends ChannelInitializer<SocketChannel> {
    private final List<NettyClient.DisconnectListener> disconnectListeners;

    private final LocalHost localHost;

    public NettyChannelInitializer(LocalHost localHost) {
        this(localHost, new ArrayList<>());
    }

    public NettyChannelInitializer(LocalHost localHost, List<NettyClient.DisconnectListener> disconnectListeners) {
        this.disconnectListeners = disconnectListeners;
        this.localHost = localHost;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline = socketChannel.pipeline();

        NettyHandler handler = new NettyHandler(localHost, disconnectListeners);
        RemoteConnection remoteConnection = handler.getRemoteConnection();

        pipeline.addLast("decoder", new RemoteObjectDecoder(remoteConnection));
        pipeline.addLast("encoder", new RemoteObjectEncoder(remoteConnection));

        pipeline.addLast("handler", handler);
    }
}
