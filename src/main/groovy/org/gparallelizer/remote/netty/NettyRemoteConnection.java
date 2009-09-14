package org.gparallelizer.remote.netty;

import org.gparallelizer.remote.RemoteConnection;
import org.gparallelizer.remote.messages.BaseMsg;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelFuture;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Connection using Netty
 *
 * @author Alex Tkachman
 */
public class NettyRemoteConnection extends RemoteConnection {
    private NettyHandler handler;
    private final MyChannelFutureListener writeListener = new MyChannelFutureListener();

    public NettyRemoteConnection(NettyTransportProvider provider, NettyHandler netHandler) {
        super(provider);
        this.handler = netHandler;
    }

    public void write(BaseMsg msg) {
        if(handler.getChannel().isConnected() && handler.getChannel().isOpen()) {
            writeListener.incrementAndGet();
            handler.getChannel().write(msg).addListener(writeListener);
        }
    }

    public void disconnect() {
        writeListener.incrementAndGet();
        writeListener.handler = handler;
        try {
            writeListener.operationComplete(null);
        } catch (Exception e) {
        }
    }

    private static class MyChannelFutureListener extends AtomicInteger implements ChannelFutureListener {
        public volatile NettyHandler handler;

        public void operationComplete(ChannelFuture future) throws Exception {
            if (decrementAndGet() == 0 && handler != null) {
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
    }
}
