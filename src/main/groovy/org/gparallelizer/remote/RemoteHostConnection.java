package org.gparallelizer.remote;

import org.gparallelizer.remote.messages.BaseMsg;
import org.gparallelizer.remote.messages.HostIdMsg;

/**
 * Represents connection to remote host
 *
 * @author Alex Tkachman
 */
public abstract class RemoteHostConnection {
    private final RemoteHostTransportProvider provider;

    private RemoteHost host;

    public RemoteHostConnection(RemoteHostTransportProvider provider) {
        this.provider = provider;
    }

    public void onMessage (BaseMsg msg) {
        if (host == null) {
            final HostIdMsg idMsg = (HostIdMsg) msg;
            host = provider.getRemoteHost(idMsg.hostId, this);
        }
        else {
            host.onMessage(msg);
        }
    }

    public void onException(Throwable cause) {
    }

    public void onConnect() {
        write(new HostIdMsg(provider));
    }

    public void onDisconnect() {
        provider.onDisconnect(host);
    }

    public abstract void write(BaseMsg msg);

    public void setHost(RemoteHost host) {
        this.host = host;
    }

    public abstract void disconnect();
}
