package groovyx.gpars.remote.netty;

import groovyx.gpars.remote.RemoteConnection;

public interface ConnectListener {
    public void onConnect(RemoteConnection connection);
}
