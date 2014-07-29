package groovyx.gpars.agent.remote;

import groovyx.gpars.agent.Agent;
import groovyx.gpars.remote.RemoteHost;
import groovyx.gpars.serial.RemoteSerialized;

public class RemoteAgent<T> extends Agent<T> implements RemoteSerialized{
    private final RemoteHost remoteHost;

    public RemoteAgent(RemoteHost remoteHost) {
        this.remoteHost = remoteHost;
    }
}
