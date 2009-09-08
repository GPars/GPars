package org.gparallelizer.remote.memory;

import org.gparallelizer.remote.RemoteNode;
import org.gparallelizer.remote.LocalNode;
import org.gparallelizer.remote.RemoteActor;
import org.gparallelizer.actors.ActorMessage;

import java.io.Serializable;
import java.io.IOException;
import java.util.UUID;

public class SharedMemoryNode extends MemoryNode {
    public SharedMemoryNode(LocalNode node) {
        super(node);
    }

    public void send(RemoteActor receiver, ActorMessage<Serializable> message) {
        throw new UnsupportedOperationException();
    }

    protected RemoteActor createRemoteActor(UUID uid) {
        if (uid == RemoteNode.MAIN_ACTOR_ID)
            return new SharedMemoryActor(this, localNode.getMainActor());

        throw new UnsupportedOperationException();
    }

    protected void deliver(byte[] bytes) throws IOException {
        throw new UnsupportedOperationException();
    }
}
