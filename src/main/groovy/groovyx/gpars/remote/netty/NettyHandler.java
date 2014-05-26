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

import static io.netty.channel.ChannelHandler.Sharable;

import io.netty.channel.*;

/**
 * @author Alex Tkachman
 */
@Sharable
public class NettyHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        super.channelRead(ctx, msg);
        System.err.println(msg);
    }
//
//    private final RemoteConnection connection;
//
//    public NettyHandler(final NettyTransportProvider provider) {
//        connection = new NettyRemoteConnection(provider, this);
//    }
//
//    @Override
//    public void channelConnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
//        connection.onConnect();
//    }
//
//    @Override
//    public void channelDisconnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
//        connection.onDisconnect();
//    }
//
//    @Override
//    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) {
//        final SerialMsg msg = (SerialMsg) e.getMessage();
//        msg.execute(connection);
//    }
//
}
