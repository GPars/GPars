//  GPars (formerly GParallelizer)
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

package groovyx.gpars.remote.netty;

import groovyx.gpars.remote.RemoteConnection;
import groovyx.gpars.serial.SerialMsg;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

/**
 * @author Alex Tkachman
 */
@ChannelPipelineCoverage("one")
public class NettyHandler extends SimpleChannelHandler {

    private Channel channel;

    private final RemoteConnection connection;

    public NettyHandler(final NettyTransportProvider provider) {
        connection = new NettyRemoteConnection(provider, this);
    }

    @Override
    public void channelOpen(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
        channel = e.getChannel();
        channel.getPipeline().addFirst("encoder", new RemoteObjectEncoder(connection));
        channel.getPipeline().addFirst("decoder", new RemoteObjectDecoder(connection));
    }

    @Override
    public void channelConnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
        connection.onConnect();
    }

    @Override
    public void channelDisconnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
        connection.onDisconnect();
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) {
        final SerialMsg msg = (SerialMsg) e.getMessage();
        msg.execute(connection);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final ExceptionEvent e) {
        //noinspection ThrowableResultOfMethodCallIgnored
        connection.onException(e.getCause());
        e.getCause().printStackTrace();
    }

    public Channel getChannel() {
        return channel;
    }
}
