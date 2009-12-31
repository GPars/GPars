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

package groovyx.gpars.actor.blocking

import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.Actors
import java.util.concurrent.CyclicBarrier

public class CrossReplyTest extends GroovyTestCase {

    public void testReplyToThreadBound() {
        volatile int result = 0
        CyclicBarrier barrier = new CyclicBarrier(2)

        Actor incrementor = Actors.actor {
            react {
                reply it + 1
            }
        }

        Actor actor = Actors.actor {
            incrementor.send 2
            receive {
                result = it
                barrier.await()
            }
        }

        barrier.await()
        assertEquals 3, result
    }

    public void testMessageReplyToThreadBound() {
        volatile int result = 0
        CyclicBarrier barrier = new CyclicBarrier(2)

        Actor incrementor = Actors.actor {
            react {
                it.reply it + 1
            }
        }

        Actor actor = Actors.actor {
            incrementor.send 2
            receive {
                result = it
                barrier.await()
            }
        }

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
        }

        Actor actor = Actors.actor {
            incrementor.send 2
            react {
                result = it
                barrier.await()
            }
        }

        barrier.await()
        assertEquals 3, result
    }

    public void testMessageReplyToEventDriven() {
        volatile int result = 0
        CyclicBarrier barrier = new CyclicBarrier(2)

        Actor incrementor = Actors.actor {
            receive {
                it.reply it + 1
            }
        }

        Actor actor = Actors.actor {
            incrementor.send 2
            react {
                result = it
                barrier.await()
            }
        }

        barrier.await()
        assertEquals 3, result
    }

}
