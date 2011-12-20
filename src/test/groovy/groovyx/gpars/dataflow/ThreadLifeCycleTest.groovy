// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
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

package groovyx.gpars.dataflow

import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.BlockingActor
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

public class ThreadLifeCycleTest extends GroovyTestCase {

    public void testPGroup() {
        final Actor actor = Actors.blockingActor {
            receive {}
        }
        assert groovyx.gpars.actor.Actors.defaultActorPGroup == actor.parallelGroup
        actor << 'Message'
    }

    public void testBasicLifeCycle() {
        AtomicInteger counter = new AtomicInteger(0)
        final CountDownLatch latch = new CountDownLatch(1)

        Actors.blockingActor {
            enhance(delegate, counter, latch)
            counter.incrementAndGet()
        }
        latch.await()
        assert 2 == counter.get()
    }

    public void testExceptionLifeCycle() {
        AtomicInteger counter = new AtomicInteger(0)
        final CountDownLatch latch = new CountDownLatch(1)

        Actors.blockingActor {
            enhance(delegate, counter, latch)
            counter.incrementAndGet()
            throw new RuntimeException('test')
        }
        latch.await()
        assert 3 == counter.get()
    }

    public void testTimeoutLifeCycle() {
        AtomicInteger counter = new AtomicInteger(0)
        final CountDownLatch latch = new CountDownLatch(1)

        Actors.blockingActor {
            enhance(delegate, counter, latch)
            counter.incrementAndGet()
            receive(10.milliseconds) {}  //will timeout
        }
        latch.await()
        assert 3 == counter.get()
    }

    private void enhance(final BlockingActor thread, final AtomicInteger counter, final CountDownLatch latch) {

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
