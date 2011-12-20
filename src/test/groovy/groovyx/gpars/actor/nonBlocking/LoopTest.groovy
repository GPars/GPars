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

package groovyx.gpars.actor.nonBlocking

import groovyx.gpars.actor.Actor
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.group.PGroup
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import static groovyx.gpars.actor.Actors.actor

/**
 *
 * @author Vaclav Pech
 * Date: Feb 17, 2009
 */
public class LoopTest extends GroovyTestCase {

    PGroup group

    protected void setUp() {
        group = new DefaultPGroup(10)
    }

    protected void tearDown() {
        group.shutdown()
    }

    public void testLoop() {
        final def barrier = new CyclicBarrier(2)
        final AtomicInteger counter = new AtomicInteger(0)

        final Actor actor = actor {
            loop {
                react {
                    counter.incrementAndGet()
                    barrier.await()
                    react {message ->
                        counter.incrementAndGet()
                        barrier.await()

                    }
                }
            }
        }

        Thread.sleep 1000
        assert 0 == counter.intValue()

        1.upto(7) {
            actor.send 'message'
            barrier.await()
            assert it == counter.intValue()
        }
        actor.stop().join()
    }

    public void testLoopStop() {
        final def barrier = new CyclicBarrier(2)
        final def afterStopBarrier = new CyclicBarrier(2)
        final AtomicInteger counter = new AtomicInteger(0)

        final Actor actor = actor {
            loop {
                barrier.await()
                Thread.sleep 10000
                react {
                    counter.incrementAndGet()
                }
            }
        }

        actor.metaClass {
            afterStop = {List messages ->
                afterStopBarrier.await()
            }
            onInterrupt = {}
        }

        barrier.await()
        actor.send 'message'
        actor.terminate()
        afterStopBarrier.await()
        assert 0 == counter.intValue()
    }

    public void testSubsequentLoopStop() {
        final def barrier = new CyclicBarrier(2)
        final def afterBarrier = new CyclicBarrier(2)
        final AtomicInteger counter = new AtomicInteger(0)
        AtomicReference<List> messagesReference = new AtomicReference<List>(null)

        final Actor actor = actor {
            loop {
                barrier.await()
                react {
                    counter.incrementAndGet()
                    barrier.await()
                    Thread.sleep 10000
                }
            }
        }

        actor.metaClass {
            afterStop = {List messages ->
                messagesReference.set(messages)
                afterBarrier.await()
            }
            onInterrupt = {}
        }

        actor.send 'message'
        barrier.await()
        actor.send 'message'
        barrier.await()
        actor.terminate()

        afterBarrier.await()
        assert 1 == counter.intValue()
        assert 1 == messagesReference.get().size()
    }

    public void testBeforeLoopStop() {
        final AtomicInteger counter = new AtomicInteger(0)

        final Actor actor = actor {
            Thread.sleep 10000
            loop {
                counter.incrementAndGet()
            }
        }

        actor.metaClass { onInterrupt = {} }

        actor.send 'message'
        actor.terminate().join()

        assert 0 == counter.intValue()
    }
}
