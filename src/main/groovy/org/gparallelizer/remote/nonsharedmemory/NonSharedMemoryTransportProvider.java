package org.gparallelizer.remote.nonsharedmemory;

import org.gparallelizer.remote.RemoteTransportProvider;
import org.gparallelizer.remote.LocalNode;

public class NonSharedMemoryTransportProvider extends RemoteTransportProvider<NonSharedMemoryNode> {

    private final static RemoteTransportProvider instance = new NonSharedMemoryTransportProvider();

    public static RemoteTransportProvider getInstance() {
        return instance;
    }

    protected NonSharedMemoryNode createRemoteNode(LocalNode node) {
        return new NonSharedMemoryNode(node);
    }
}