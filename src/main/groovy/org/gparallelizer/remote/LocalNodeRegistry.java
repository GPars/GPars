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

import org.gparallelizer.remote.memory.SharedMemoryTransportProvider;

import java.util.*;

public class LocalNodeRegistry {
    public static final Set<RemoteTransportProvider> transportProviders
            = Collections.synchronizedSet(new HashSet<RemoteTransportProvider>(Arrays.asList(SharedMemoryTransportProvider.getInstance())));

    public synchronized static void connect(final LocalNode node) {
        for (final RemoteTransportProvider transportProvider : transportProviders) {
            node.connect(transportProvider);
        }
    }

    public synchronized static void disconnect(final LocalNode node) {
        for (final RemoteTransportProvider transportProvider : transportProviders) {
            node.getScheduler().execute(new Runnable(){
                public void run() {
                    transportProvider.disconnect(node);
                }
            });
        }
    }

    public static synchronized void removeTransportProvider (RemoteTransportProvider provider) {
        transportProviders.remove(provider);
    }

    public static synchronized void addTransportProvider (RemoteTransportProvider provider) {
        transportProviders.add(provider);
    }
}
