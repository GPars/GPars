package org.gparallelizer.remote.net;

import org.gparallelizer.remote.RemoteNode;
import org.gparallelizer.remote.LocalNode;
import org.gparallelizer.actors.Actor;

import java.util.UUID;
import java.io.IOException;

public class NetNode extends RemoteNode {
    private final LocalNode localNode;

    public NetNode(final LocalNode node) {
        super();
        this.localNode = node;

        final Actor main = localNode.getMainActor();
        if (main != null)
            localActorsId.put(MAIN_ACTOR_ID, main);
    }

    public UUID getId() {
        return localNode.getId();
    }

    public void onConnect(RemoteNode node) {
        localNode.onConnect(node);
    }

    public void onDisconnect(RemoteNode node) {
        localNode.onDisconnect(node);
    }

    protected void deliver(byte[] bytes) throws IOException {
        onMessageReceived(bytes, localNode.getScheduler());
    }
}