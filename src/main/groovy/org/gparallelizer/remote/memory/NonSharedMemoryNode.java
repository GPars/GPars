package org.gparallelizer.remote.memory;

import org.gparallelizer.remote.LocalNode;
import org.gparallelizer.actors.Actor;

import java.io.IOException;

public class NonSharedMemoryNode extends MemoryNode {
    private final LocalNode localNode;

    public NonSharedMemoryNode(final LocalNode node) {
        super(node);
        this.localNode = node;

        final Actor main = localNode.getMainActor();
        if (main != null)
            localActorsId.put(MAIN_ACTOR_ID, main);
    }

    protected void deliver(byte[] bytes) throws IOException {
        onMessageReceived(bytes, localNode.getScheduler());
    }
}