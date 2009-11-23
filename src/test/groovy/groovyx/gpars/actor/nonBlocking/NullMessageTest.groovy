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
import groovyx.gpars.actor.impl.AbstractPooledActor
import groovyx.gpars.dataflow.DataFlowVariable
import java.util.concurrent.CountDownLatch
import static groovyx.gpars.actor.Actors.actor

public class NullMessageTest extends GroovyTestCase {
    public void testNullMesage() {
        volatile def result = ''
        final def latch = new CountDownLatch(1)
        final AbstractPooledActor actor = actor {
            react {
                result = it
                latch.countDown()
            }
        }
        actor << null
        latch.await()
        assertNull result
    }

    public void testNullMesageFromActor() {
        volatile def result = ''
        final def latch = new CountDownLatch(1)
        final AbstractPooledActor actor = actor {
            react {
                result = it
                latch.countDown()
            }
        }
        Actors.actor {
            actor << null
            latch.await()
        }
        latch.await()
        assertNull result
    }

    public void testNullMesageFromActorWithReply() {
        final def result = new DataFlowVariable()
        final AbstractPooledActor actor = actor {
            react {
                reply 10
            }
        }
        Actors.actor {
            actor << null
            react {
                result << it
            }
        }
        assertEquals 10, result.val
    }
}
