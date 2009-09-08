package org.gparallelizer.remote.memory;

import org.gparallelizer.remote.memory.SharedMemoryNode;
import org.gparallelizer.remote.LocalNode;

public class SharedMemoryTransportProvider extends MemoryTransportProvider<MemoryNode> {
    private final static SharedMemoryTransportProvider instance = new SharedMemoryTransportProvider();

    public static SharedMemoryTransportProvider getInstance() {
        return instance;
    }

    protected MemoryNode createRemoteNode(LocalNode node) {
        return new SharedMemoryNode(node);
    }
}
