package org.gparallelizer.remote.sharedmemory;

import org.gparallelizer.remote.sharedmemory.SharedMemoryNode;
import org.gparallelizer.remote.RemoteTransportProvider;
import org.gparallelizer.remote.LocalNode;

import java.util.*;

public class SharedMemoryTransportProvider extends RemoteTransportProvider {
    private final static SharedMemoryTransportProvider instance = new SharedMemoryTransportProvider();

    private final Map<UUID,SharedMemoryNode> registry = new HashMap<UUID,SharedMemoryNode>();

    public synchronized void connect(final LocalNode node) {
        final SharedMemoryNode smn = new SharedMemoryNode(node);
        registry.put(node.getId(), smn);

        for (final SharedMemoryNode n : registry.values()) {
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
        final SharedMemoryNode smn = registry.remove(node.getId());

        for (final SharedMemoryNode n : registry.values()) {
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

    public static SharedMemoryTransportProvider getInstance() {
        return instance;
    }
}
