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

package groovyx.gpars.actor.nonBlocking

import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.PooledActorGroup
import groovyx.gpars.actor.ReactiveActor
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

public class ReactorTest extends GroovyTestCase {

    public void testSimple() {
        def res = []
        CountDownLatch latch = new CountDownLatch(6)
        final def processor = Actors.reactor {
            res << 2 * it
            latch.countDown()
        }

        (0..5).each {
          processor << it
        }

        latch.await()
        processor.stop()
        processor.join()

        assertEquals ([0, 2, 4, 6, 8, 10], res)
    }

    public void testWait() {
        final def processor = Actors.reactor {
            2 * it
        }

        assertEquals (20, processor.sendAndWait(10))
        assertEquals (40, processor.sendAndWait(20))
        assertEquals (60, processor.sendAndWait(30))

        processor.stop()
        processor.join(10,TimeUnit.SECONDS)
    }

    public void testMessageProcessing() {
        final def group = new PooledActorGroup(4)
        final def result1 = new AtomicInteger(0)
        final def result2 = new AtomicInteger(0)
        final def result3 = new AtomicInteger(0)

        final def processor = group.reactor {
            2 * it
        }

        final def a1 = group.actor {
            result1 = processor.sendAndWait(10)
        }

        final def a2 = group.actor {
            result2 = processor.sendAndWait(20)
        }

        final def a3 = group.actor {
            result3 = processor.sendAndWait(30)
        }

        [a1, a2, a3]*.join()
        assertEquals 20, result1
        assertEquals 40, result2
        assertEquals 60, result3

        processor.stop()
        processor.join()
    }

    public void testGroup() {
        final PooledActorGroup group = new PooledActorGroup()
        final ReactiveActor reactor = group.reactor {}
        assertSame group, reactor.actorGroup
    }
}
