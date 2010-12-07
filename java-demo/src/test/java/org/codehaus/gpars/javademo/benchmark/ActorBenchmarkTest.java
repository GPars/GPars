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

package org.codehaus.gpars.javademo.benchmark;

import groovyx.gpars.actor.Actor;
import groovyx.gpars.actor.DynamicDispatchActor;
import groovyx.gpars.group.DefaultPGroup;
import groovyx.gpars.group.PGroup;
import groovyx.gpars.scheduler.DefaultPool;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Creates 10000 chained Actors that are passing messages along. Messages are passed to the first actor and the messages are propagated
 * to the others.
 *
 * @author Vaclav Pech, Lukas Krecan, Pavel Jetensky, Michal Franc
 */
@SuppressWarnings({"MagicNumber"})
public class ActorBenchmarkTest {
    private static final int ACTORS = 10000;
    private static final int MESSAGES = 500;

    @Test
    public void testBenchmark() throws Exception {

        final int concurrencyLevel = 20;

        // All actors in this group share the same thread pool
        final PGroup group = new DefaultPGroup(new DefaultPool(true, concurrencyLevel));

        final long t1 = System.currentTimeMillis();
        // With each message received counter is decreased by the actors
        final int latchCount = ACTORS * MESSAGES;
        final CountDownLatch cdl = new CountDownLatch(latchCount);
        Actor lastActor = null;

        // Create and chain actors (backwards - the lastActor will be first)
        for (int i = 0; i < ACTORS; i++) {
            final Actor actor = new MessagePassingActor(lastActor, cdl);
            actor.setParallelGroup(group);
            lastActor = actor;
            actor.start();
        }

        for (int i = 0; i < MESSAGES; i++) {
            lastActor.send("Hi");
        }

        cdl.await(1000, TimeUnit.SECONDS);

        group.shutdown();
        final long t2 = System.currentTimeMillis();
        System.out.println("Time to process " + latchCount + " messages: " + (t2 - t1) + " ms");
        assertEquals("Latch has not been decreased to 0", 0, cdl.getCount());

    }

    /**
     * Actor implementation.
     */
    public static class MessagePassingActor extends DynamicDispatchActor {

        private final Actor soFarLast;
        private final CountDownLatch cdl;

        MessagePassingActor(final Actor soFarLast, final CountDownLatch cdl) {
            this.soFarLast = soFarLast;
            this.cdl = cdl;
        }

        /**
         * Pass message to next actor and decrease the latch
         *
         * @param msg
         * @return
         */
        Object onMessage(final Object msg) {
            if (soFarLast != null) soFarLast.send(msg);
            cdl.countDown();
            return null;
        }
    }
}
