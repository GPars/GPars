package org.gparallelizer.remote.netty;

import org.gparallelizer.remote.RemoteHostConnection;
import org.gparallelizer.remote.messages.BaseMsg;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelFuture;

import java.util.concurrent.CountDownLatch;

/**
 * Connection using Netty
 *
 * @author Alex Tkachman
 */
public class NettyRemoteConnection extends RemoteHostConnection {
    private NettyHandler handler;

    public NettyRemoteConnection(NettyTransportProvider provider, NettyHandler netHandler) {
        super(provider);
        this.handler = netHandler;
    }

    public void write(BaseMsg msg) {
        handler.getChannel().write(msg);
    }

    public void disconnect() {
        final CountDownLatch cdl = new CountDownLatch(1);
        handler.getChannel().close().addListener(new ChannelFutureListener(){
            public void operationComplete(ChannelFuture future) throws Exception {
                cdl.countDown();
            }
        });
        try {
            cdl.await ();
        } catch (InterruptedException e) {//
            e.printStackTrace();
        }
    }
}
