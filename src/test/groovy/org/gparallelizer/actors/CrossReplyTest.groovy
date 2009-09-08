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

package org.gparallelizer.actors

import org.gparallelizer.actors.pooledActors.PooledActors
import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import java.util.concurrent.CyclicBarrier

public class CrossReplyTest extends GroovyTestCase {

    public void testReplyToThreadBound() {
        volatile int result = 0
        CyclicBarrier barrier = new CyclicBarrier(2)

        Actor incrementor = PooledActors.actor {
            react {
                reply it + 1
            }
        }
        incrementor.start()

        Actor actor = Actors.actor {
            incrementor.send 2
            receive {
                result = it
                barrier.await()
                stop()
            }
        }
        actor.start()

        barrier.await()
        incrementor.stop()
        assertEquals 3, result
    }

    public void testMessageReplyToThreadBound() {
        volatile int result = 0
        CyclicBarrier barrier = new CyclicBarrier(2)

        Actor incrementor = PooledActors.actor {
            react {
                it.reply it + 1
            }
        }
        incrementor.start()

        Actor actor = Actors.actor {
            incrementor.send 2
            receive {
                result = it
                barrier.await()
                stop()
            }
        }
        actor.start()

        barrier.await()
        assertEquals 3, result
    }

    public void testReplyToEventDriven() {
        volatile int result = 0
        CyclicBarrier barrier = new CyclicBarrier(2)

        Actor incrementor = Actors.actor {
            receive {
                reply it + 1
            }
            stop()
        }
        incrementor.start()

        Actor actor = PooledActors.actor {
            incrementor.send 2
            react {
                result = it
                barrier.await()
            }
        }
        actor.start()

        barrier.await()
        incrementor.stop()
        assertEquals 3, result
    }

    public void testMessageReplyToEventDriven() {
        volatile int result = 0
        CyclicBarrier barrier = new CyclicBarrier(2)

        Actor incrementor = Actors.actor {
            receive {
                it.reply it + 1
            }
            stop()
        }
        incrementor.start()

        Actor actor = PooledActors.actor {
            incrementor.send 2
            react {
                result = it
                barrier.await()
            }
        }
        actor.start()

        barrier.await()
        assertEquals 3, result
    }

}
