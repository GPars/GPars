package org.gparallelizer.remote;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

public abstract class RemoteTransportProvider<T extends RemoteNode> {
    protected final Map<UUID,T> registry = new HashMap<UUID, T>();

    protected void connect(LocalNode local, T remote) {
        local.onConnect(remote);
    }

    protected void disconnect(LocalNode local, T remote) {
        local.onDisconnect(remote);
    }

    public synchronized void connect(final LocalNode node) {
        for (final T n : registry.values()) {
          connect(node, n);
        }
    }

    public synchronized void disconnect(final LocalNode node) {
        for (final T n : registry.values()) {
            disconnect(node, n);
        }
    }
}
