package org.gparallelizer.remote;

import org.gparallelizer.actors.ActorMessage;

import java.io.Serializable;

/**
 * Representation of remote node
 */
public abstract class RemoteNode {
    private final RemoteTransport transport;

    public RemoteNode(RemoteTransport transport) {
        this.transport = transport;
    }

    public final void send(RemoteActor receiver, ActorMessage<Serializable> message) {
        transport.send(receiver, message);
    }

    protected void onDisconnect() {
    }

    public RemoteTransport getTransport() {
        return transport;
    }
}
