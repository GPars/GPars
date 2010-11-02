// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.util;

import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of the message queue for actor and agent messaging
 *
 * @author Vaclav Pech
 */
public final class MessagingQueue {
    private final List<Object> queue = new ArrayList<Object>(10);

    synchronized boolean isEmpty() {
        return queue.isEmpty();
    }

    synchronized Object poll() {
        return queue.isEmpty() ? null : queue.remove(0);
    }

    synchronized void add(final Object element) {
        queue.add(element);
    }
}

//todo test