package org.gparallelizer.remote.netty;

import org.gparallelizer.remote.RemoteHostConnection;
import org.gparallelizer.remote.messages.BaseMsg;

/**
 * Connection using Netty
 *
 * @author Alex Tkachman
 */
public class NettyRemoteConnection extends RemoteHostConnection {
    private NettyTransportProvider.Handler handler;

    public NettyRemoteConnection(NettyTransportProvider provider, NettyTransportProvider.Handler netHandler) {
        super(provider);
        this.handler = netHandler;
    }

    public void write(BaseMsg msg) {
        handler.getChannel().write(msg);
    }
}
