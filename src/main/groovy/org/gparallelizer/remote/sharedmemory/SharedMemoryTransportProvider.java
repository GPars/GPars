package org.gparallelizer.remote.sharedmemory;

import org.gparallelizer.remote.sharedmemory.SharedMemoryNode;
import org.gparallelizer.remote.RemoteTransportProvider;
import org.gparallelizer.remote.LocalNode;

import java.util.*;

public class SharedMemoryTransportProvider extends RemoteTransportProvider<SharedMemoryNode> {
    private final static SharedMemoryTransportProvider instance = new SharedMemoryTransportProvider();

    public static SharedMemoryTransportProvider getInstance() {
        return instance;
    }

    protected SharedMemoryNode createRemoteNode(LocalNode node) {
        return new SharedMemoryNode(node);
    }
}
