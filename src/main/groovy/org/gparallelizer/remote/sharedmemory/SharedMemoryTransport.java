package org.gparallelizer.remote.sharedmemory;

import org.gparallelizer.actors.Actor;
import org.gparallelizer.actors.ActorMessage;
import org.gparallelizer.remote.RemoteTransport;
import org.gparallelizer.remote.RemoteNode;

import java.io.IOException;
import java.io.Serializable;

public class SharedMemoryTransport extends RemoteTransport {
    public SharedMemoryTransport() {
        super(null);
    }

    public SharedMemoryTransport(RemoteNode node) {
        super(node);
    }

    protected void deliver(byte[] bytes) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void send(Actor receiver, ActorMessage<Serializable> message) {
        receiver.send(message.getPayLoad());
    }

    public void setNode(RemoteNode node) {
        this.node = node;
    }
}
