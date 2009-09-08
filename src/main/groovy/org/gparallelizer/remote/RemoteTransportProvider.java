package org.gparallelizer.remote;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

public abstract class RemoteTransportProvider<T extends RemoteNode> {
    private final Map<UUID,T> registry = new HashMap<UUID, T>();

    protected abstract T createRemoteNode(LocalNode node);

    public synchronized void connect(final LocalNode node) {
        final T smn = createRemoteNode(node);

        for (final T n : registry.values()) {
            node.getScheduler().execute(new Runnable(){
                public void run() {
                    node.onConnect(n);
                    n.onConnect(smn);
                }
            });
        }

        registry.put(node.getId(), smn);
    }

    public synchronized void disconnect(final LocalNode node) {
        final T smn = registry.remove(node.getId());

        for (final T n : registry.values()) {
            node.getScheduler().execute(new Runnable(){
                public void run() {
                    node.onDisconnect(n);
                    n.onDisconnect(smn);
                }
            });
        }
    }
}
