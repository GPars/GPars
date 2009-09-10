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

import org.gparallelizer.remote.LocalNode;
import org.gparallelizer.remote.RemoteNode;
import org.gparallelizer.remote.RemoteTransportProvider;

/**
 * @author Alex Tkachman
 */
public abstract class MemoryTransportProvider extends RemoteTransportProvider {
    protected abstract RemoteNode createRemoteNode(LocalNode node);

    public void connect(final LocalNode node) {
        super.connect(node);

        synchronized (registry) {
            registry.put(node.getId(), getLocalRemote(node, true));

            for (final RemoteNode n : registry.values()) {
                if (!n.getId().equals(node.getId())) {
                    node.onConnect(n);
                    n.onConnect(node);
                }
            }
        }
    }

    public synchronized void disconnect(final LocalNode node) {
        synchronized (registry) {
            for (final RemoteNode n : registry.values()) {
                if (!n.getId().equals(node.getId())) {
                    node.onDisconnect(n);
                    n.onDisconnect(node);
                }
            }

            registry.remove(node.getId());
        }

        super.disconnect(node);
    }

    public RemoteNode getLocalRemote(LocalNode local, boolean force) {
        synchronized (registry) {
            RemoteNode localRemote = registry.get(local.getId());
            if (localRemote == null && force) {
                localRemote = createRemoteNode(local);
                registry.put(local.getId(), localRemote);
            }
            return localRemote;
        }
    }
}
