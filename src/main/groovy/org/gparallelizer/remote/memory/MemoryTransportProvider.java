//  GParallelizer
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License. 

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
