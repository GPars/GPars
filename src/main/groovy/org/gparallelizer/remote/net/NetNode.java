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

package org.gparallelizer.remote.net;

import org.gparallelizer.remote.RemoteNode;
import org.gparallelizer.remote.LocalNode;
import org.gparallelizer.actors.Actor;

import java.util.UUID;
import java.io.IOException;

public class NetNode extends RemoteNode {
    private final LocalNode localNode;

    public NetNode(final LocalNode node) {
        super();
        this.localNode = node;

        final Actor main = localNode.getMainActor();
        if (main != null)
            localActorsId.put(MAIN_ACTOR_ID, main);
    }

    public UUID getId() {
        return localNode.getId();
    }

    public void onConnect(RemoteNode node) {
        localNode.onConnect(node);
    }

    public void onDisconnect(RemoteNode node) {
        localNode.onDisconnect(node);
    }

    protected void deliver(byte[] bytes) throws IOException {
        onMessageReceived(bytes, localNode.getScheduler());
    }
}
