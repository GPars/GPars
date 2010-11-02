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
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;


/**
 * An implementation of the message queue for actor and agent messaging.
 * It leverages the fact that in any moment there's only one reading thread accessing the queue
 * and that potential read thread swap at the actor or agent thread pool synchronizes thread memory.
 *
 * @author Vaclav Pech
 */
@SuppressWarnings({"SynchronizedMethod", "FieldAccessedSynchronizedAndUnsynchronized"})
public final class MessagingQueue {

    private List<Object> outside = new ArrayList<Object>(INITIAL_CAPACITY);
    private List<Object> inside = new ArrayList<Object>(INITIAL_CAPACITY);
    @SuppressWarnings({"UnusedDeclaration", "FieldMayBeFinal"})
    private volatile int counter = 0;
    private static final AtomicIntegerFieldUpdater<MessagingQueue> counterUpdater = AtomicIntegerFieldUpdater.newUpdater(
            MessagingQueue.class, "counter");
    private static final int INITIAL_CAPACITY = 50;

    boolean isEmpty() {
        return counterUpdater.get(this) == 0;
    }

    @SuppressWarnings({"SynchronizeOnThis"})
    Object poll() {
        if (!inside.isEmpty()) {
            counterUpdater.decrementAndGet(this);
            return inside.remove(0);
        }
        final List<Object> localQueue = inside;
        inside = outside;
        synchronized (this) {
            outside = localQueue;
        }
        if (!inside.isEmpty()) {
            counterUpdater.decrementAndGet(this);
            return inside.remove(0);
        }
        return null;
    }

    @SuppressWarnings({"AccessToStaticFieldLockedOnInstance"})
    synchronized void add(final Object element) {
        outside.add(element);
        counterUpdater.incrementAndGet(this);
    }
}