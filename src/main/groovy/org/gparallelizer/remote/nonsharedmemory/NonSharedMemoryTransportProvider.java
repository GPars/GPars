package org.gparallelizer.remote.nonsharedmemory;

import org.gparallelizer.remote.sharedmemory.SharedMemoryNode;
import org.gparallelizer.remote.RemoteTransportProvider;
import org.gparallelizer.remote.LocalNode;

import java.util.*;

public class NonSharedMemoryTransportProvider extends RemoteTransportProvider {
    private final Map<UUID,NonSharedMemoryNode> registry = new HashMap<UUID, NonSharedMemoryNode>();

    private final static RemoteTransportProvider instance = new NonSharedMemoryTransportProvider();

    public synchronized void connect(final LocalNode node) {
        final NonSharedMemoryNode smn = new NonSharedMemoryNode(node);
        registry.put(node.getId(), smn);

        for (final NonSharedMemoryNode n : registry.values()) {
            if (n.getLocalNode() == node)
                continue;

            node.getScheduler().execute(new Runnable(){
                public void run() {
                    node.onConnect(n);
                    n.getLocalNode().onConnect(smn);
                }
            });
        }
    }

    public synchronized void disconnect(final LocalNode node) {
        final NonSharedMemoryNode smn = registry.remove(node.getId());

        for (final NonSharedMemoryNode n : registry.values()) {
            if (n.getLocalNode() == node)
                continue;

            node.getScheduler().execute(new Runnable(){
                public void run() {
                    node.onDisconnect(n);
                    n.onDisconnect(smn);
                }
            });
        }
    }

    public static RemoteTransportProvider getInstance() {
        return instance;
    }
}