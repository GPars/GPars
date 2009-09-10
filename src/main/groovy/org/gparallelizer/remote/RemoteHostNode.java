package org.gparallelizer.remote;

import org.gparallelizer.actors.Actor;

import java.io.IOException;
import java.io.Serializable;
import java.util.UUID;

/**
 * @author Alex Tkachman
 */
public class RemoteHostNode<T extends RemoteHostTransportProvider> extends RemoteNode<T> {
    private final RemoteHost remoteHost;

    public RemoteHostNode(UUID id, RemoteHost remoteHost) {
        super(id, (T) remoteHost.getProvider());
        this.remoteHost = remoteHost;
    }

    public RemoteHost getRemoteHost() {
        return remoteHost;
    }

    @Override
    protected void send(RemoteActor receiver, Serializable message, Actor sender) {
        remoteHost.send(createMessage(receiver, message, sender));
    }

    public void onConnect(LocalNode node) {
        // host takes care
    }

    public void onDisconnect(LocalNode node) {
        // host takes care
    }

    protected RemoteActor createRemoteActor(UUID uid) {
        return new RemoteActor(this, uid);
    }

    protected void deliver(byte[] bytes) throws IOException {
        throw new UnsupportedOperationException();
    }
}
