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
