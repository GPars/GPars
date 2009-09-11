package org.gparallelizer.remote.netty;

import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.gparallelizer.remote.RemoteHostConnection;
import org.gparallelizer.remote.messages.BaseMsg;

/**
 * @author Alex Tkachman
 */
@ChannelPipelineCoverage("one")
public class NettyHandler extends SimpleChannelHandler {

    private Channel channel;

    private final RemoteHostConnection connection;

    public NettyHandler(NettyTransportProvider provider) {
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
