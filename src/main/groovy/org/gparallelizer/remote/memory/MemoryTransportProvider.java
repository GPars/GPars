package org.gparallelizer.remote.memory;

import org.gparallelizer.remote.RemoteTransportProvider;
import org.gparallelizer.remote.RemoteNode;
import org.gparallelizer.remote.LocalNode;

public abstract class MemoryTransportProvider<T extends MemoryNode> extends RemoteTransportProvider<T>{
    protected abstract T createRemoteNode(LocalNode node);

    public synchronized void connect(final LocalNode node) {
        getLocalRemote(node, true);
        super.connect(node);
    }

    public synchronized void disconnect(final LocalNode node) {
        super.disconnect(node);
        registry.remove(node.getId());
    }

    protected void connect(LocalNode local, T remote) {
        if (local.getId().equals(remote.getId()))
            return;

        super.connect(local, remote);

        remote.onConnect(getLocalRemote(local, true));
    }

    protected void disconnect(LocalNode local, T remote) {
        if (local.getId().equals(remote.getId()))
            return;
        
        super.disconnect(local, remote);

        final RemoteNode localRemote = getLocalRemote(local, false);
        if (localRemote != null)
            remote.onDisconnect(localRemote);
    }

    private T getLocalRemote(LocalNode local, boolean force) {
        T localRemote = registry.get(local.getId());
        if (localRemote == null && force) {
            localRemote = createRemoteNode(local);
            registry.put(local.getId(), localRemote);
        }
        return localRemote;
    }
}
