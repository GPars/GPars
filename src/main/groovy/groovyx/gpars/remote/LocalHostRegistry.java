//  GPars (formerly GParallelizer)
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

package groovyx.gpars.remote;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Registry of local hosts
 *
 * @author Alex Tkachman
 */
public class LocalHostRegistry {
    public static final Set<LocalHost> localHosts
            = Collections.synchronizedSet(new HashSet<LocalHost>());

    public static synchronized void connect(final LocalNode node) {
        for (final LocalHost transportProvider : localHosts) {
            node.connect(transportProvider);
        }
    }

    public static synchronized void disconnect(final LocalNode node) {
        for (final LocalHost transportProvider : localHosts) {
            node.getScheduler().execute(new Runnable() {
                public void run() {
                    transportProvider.disconnect(node);
                }
            });
        }
    }

    public static synchronized void removeLocalHost(LocalHost localHost) {
        localHosts.remove(localHost);
    }

    public static synchronized void addLocalHost(LocalHost localHost) {
        localHosts.add(localHost);
    }
}
