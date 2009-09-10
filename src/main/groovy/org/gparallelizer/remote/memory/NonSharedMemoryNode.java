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
import org.gparallelizer.remote.messages.MessageToActor;

import java.io.IOException;

public class NonSharedMemoryNode extends MemoryNode {
    private final LocalNode localNode;

    public NonSharedMemoryNode(final LocalNode node, NonSharedMemoryTransportProvider provider) {
        super(node, provider);
        this.localNode = node;
    }

    protected void deliver(byte[] bytes) throws IOException {
        final MessageToActor remoteMessage = decodeMessage(bytes);
        localNode.getScheduler().execute(new Runnable(){
            public void run() {
                getProvider().getLocalActor(remoteMessage.getTo()).send(remoteMessage.getPayload());
            }
        });
    }
}
