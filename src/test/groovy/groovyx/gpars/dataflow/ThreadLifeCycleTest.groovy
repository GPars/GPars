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

package groovyx.gpars.dataflow

import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.impl.AbstractPooledActor
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import static groovyx.gpars.dataflow.DataFlow.start

public class ThreadLifeCycleTest extends GroovyTestCase {

    public void testActorGroup() {
        final Actor actor = start {
            react {}
        }
        assertEquals DataFlow.DATA_FLOW_GROUP, actor.actorGroup
        actor << 'Message'
    }

    public void testBasicLifeCycle() {
        AtomicInteger counter = new AtomicInteger(0)
        final CountDownLatch latch = new CountDownLatch(1)

        final def thread = start {
            enhance(delegate, counter, latch)
            counter.incrementAndGet()
        }
        latch.await()
        assertEquals 2, counter.get()
    }

    public void testExceptionLifeCycle() {
        AtomicInteger counter = new AtomicInteger(0)
        final CountDownLatch latch = new CountDownLatch(1)

        final def thread = start {
            enhance(delegate, counter, latch)
            counter.incrementAndGet()
            if (true) throw new RuntimeException('test')
        }
        latch.await()
        assertEquals 3, counter.get()
    }

    public void testTimeoutLifeCycle() {
        AtomicInteger counter = new AtomicInteger(0)
        final CountDownLatch latch = new CountDownLatch(1)

        final def thread = start {
            enhance(delegate, counter, latch)
            counter.incrementAndGet()
            react(10.milliseconds) {}  //will timeout
        }
        latch.await()
        assertEquals 3, counter.get()
    }

    private void enhance(final AbstractPooledActor thread, final AtomicInteger counter, final CountDownLatch latch) {

        thread.metaClass {
            afterStart = {->  //won't be called
                counter.incrementAndGet()
            }

            afterStop = {List undeliveredMessages ->
                counter.incrementAndGet()
                latch.countDown()
            }

            onInterrupt = {InterruptedException e ->
                counter.incrementAndGet()
            }

            onTimeout = {->
                counter.incrementAndGet()
            }

            onException = {Exception e ->
                counter.incrementAndGet()
            }
        }
    }

}
