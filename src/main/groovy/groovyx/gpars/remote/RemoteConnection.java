package groovyx.gpars.remote;

import groovyx.gpars.remote.message.HostIdMsg;
import groovyx.gpars.serial.SerialMsg;

/**
 * Represents connection to remote host
 *
 * @author Alex Tkachman
 */
public abstract class RemoteConnection {
    private final LocalHost localHost;

    private RemoteHost host;

    public RemoteConnection(LocalHost provider) {
        this.localHost = provider;
    }

    public void onMessage(SerialMsg msg) {
        if (host == null) {
            final HostIdMsg idMsg = (HostIdMsg) msg;
            host = (RemoteHost) localHost.getSerialHost(idMsg.hostId, this);
        } else {
            throw new IllegalStateException("Unexpected message: " + msg);
        }
    }

    public void onException(Throwable cause) {
    }

    public void onConnect() {
        write(new HostIdMsg(localHost.getId()));
    }

    public void onDisconnect() {
        localHost.onDisconnect(host);
    }

    public abstract void write(SerialMsg msg);

    public RemoteHost getHost() {
        return host;
    }

    public void setHost(RemoteHost host) {
        this.host = host;
    }

    public abstract void disconnect();
}
