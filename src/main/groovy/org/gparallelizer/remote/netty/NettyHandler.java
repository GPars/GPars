package org.gparallelizer.remote.netty;

import org.jboss.netty.channel.*;
import org.gparallelizer.remote.RemoteConnection;
import org.gparallelizer.remote.netty.RemoteObjectEncoder;
import org.gparallelizer.remote.netty.RemoteObjectDecoder;
import org.gparallelizer.remote.messages.BaseMsg;

/**
 * @author Alex Tkachman
 */
@ChannelPipelineCoverage("one")
public class NettyHandler extends SimpleChannelHandler {

    private Channel channel;

    private final RemoteConnection connection;

    public NettyHandler(NettyTransportProvider provider) {
        connection = new NettyRemoteConnection(provider, this);
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        channel = e.getChannel();
        channel.getPipeline().addFirst("encoder", new RemoteObjectEncoder(connection));
        channel.getPipeline().addFirst("decoder", new RemoteObjectDecoder(connection));
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
        e.getCause().printStackTrace();
    }

    public Channel getChannel() {
        return channel;
    }
}
