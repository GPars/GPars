// GPars (formerly GParallelizer)
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

package groovyx.gpars.agent;

import groovyx.gpars.util.PoolUtils;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by IntelliJ IDEA.
 * User: Vaclav
 * Date: 13.4.2010
 * Time: 12:24:34
 * To change this template use File | Settings | File Templates.
 */
public abstract class AgentHelper {
    private static ExecutorService pool = Executors.newFixedThreadPool(PoolUtils.retrieveDefaultPoolSize(), new AgentThreadFactory());

    final ConcurrentLinkedQueue<Object> msgs = new ConcurrentLinkedQueue<Object>();

    AtomicBoolean active = new AtomicBoolean(false);

    public final void send(final Object message) {
        msgs.add(message);
        schedule();
    }

    public final void leftShift(final Object message) {
        send(message);
    }

    final void perform() {
        try {
            final Object message = msgs.poll();
            if (message != null) this.handleMessage(message);
        } finally {
            active.set(false);
            schedule();
        }
    }

    abstract void handleMessage(final Object message);

    void schedule() {
        if (!msgs.isEmpty() && active.compareAndSet(false, true)) {
            pool.submit(new Runnable() {
                public void run() {
                    try {
                        AgentHelper.this.perform();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

}
