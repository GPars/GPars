package org.gparallelizer.remote.memory;

import org.gparallelizer.remote.RemoteNode;
import org.gparallelizer.remote.LocalNode;

import java.util.UUID;

public abstract class MemoryNode extends RemoteNode {
    protected final LocalNode localNode;

    public MemoryNode(LocalNode node) {
        super();
        localNode = node;
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
}
