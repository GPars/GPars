// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10, 2014  The original author or authors
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

import groovyx.gpars.remote.RemoteConnection;
import groovyx.gpars.serial.SerialMsg;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Connection using Netty
 *
 * @author Alex Tkachman
 */
public class NettyRemoteConnection extends RemoteConnection {
    private final NettyHandler handler;
    //private final MyChannelFutureListener writeListener = new MyChannelFutureListener();

    public NettyRemoteConnection(final NettyTransportProvider provider, final NettyHandler netHandler) {
        super(provider);
        this.handler = netHandler;
    }

    @Override
    public void write(final SerialMsg msg) {
        throw new NotImplementedException();
//        if (handler.getChannel().isActive() && handler.getChannel().isOpen()) {
//            writeListener.incrementAndGet();
//            handler.getChannel().write(msg).addListener(writeListener);
//        }
    }

    @Override
    public void disconnect() {
        throw new NotImplementedException();
//        writeListener.incrementAndGet();
//        writeListener.handler = handler;
//        try {
//            writeListener.operationComplete(null);
//        } catch (Exception ignored) {
//        }
    }

    /*private static class MyChannelFutureListener extends AtomicInteger implements ChannelFutureListener {
        private static final long serialVersionUID = -3054880716233778157L;
        public volatile NettyHandler handler;

        @Override
        public void operationComplete(final ChannelFuture future) throws Exception {
            if (decrementAndGet() == 0 && handler != null) {
                final CountDownLatch cdl = new CountDownLatch(1);
                handler.getChannel().close().addListener(new ChannelFutureListener() {
                    @Override
                    @SuppressWarnings({"AnonymousClassVariableHidesContainingMethodVariable"})
                    public void operationComplete(final ChannelFuture future) throws Exception {
                        cdl.countDown();
                    }
                });
                try {
                    cdl.await();
                } catch (InterruptedException e) {//
                    e.printStackTrace();
                }
            }
        }
    }*/
}
