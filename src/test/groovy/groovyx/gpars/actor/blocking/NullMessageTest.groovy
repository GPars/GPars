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

package groovyx.gpars.actor.blocking

import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.Actors
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.group.PGroupBuilder
import groovyx.gpars.scheduler.DefaultPool
import java.util.concurrent.CountDownLatch

public class NullMessageTest extends GroovyTestCase {
    public void testNullMessage() {
        def result = ''
        final def latch = new CountDownLatch(1)
        final Actor actor = Actors.blockingActor {
            receive {
                result = it
                latch.countDown()
            }
        }
        actor << null
        latch.await()
        assertNull result
    }

    public void testNullMessageFromActor() {
        final def group = PGroupBuilder.createFromPool(new DefaultPool(true, 100))
        def result = ''
        final def latch = new CountDownLatch(1)
        final Actor actor = group.blockingActor {
            receive {
                result = it
                latch.countDown()
            }
        }
        group.actor {
            actor << null
            latch.await()
        }
        latch.await()
        assertNull result
        group.shutdown()
    }

    public void testNullMessageFromActorWithReply() {
        final def result = new DataflowVariable()
        final Actor actor = Actors.blockingActor {
            receive {
                reply 10
            }
        }
        Actors.blockingActor {
            actor << null
            receive {
                result << it
            }
        }
        assert 10 == result.val
    }
}
