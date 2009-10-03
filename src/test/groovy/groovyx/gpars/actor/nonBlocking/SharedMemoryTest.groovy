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

package groovyx.gpars.actor.nonBlocking

import java.util.concurrent.CountDownLatch
import groovyx.gpars.actor.Actor
import static groovyx.gpars.actor.Actors.actor
import groovyx.gpars.actor.Actors

public class SharedMemoryTest extends GroovyTestCase {

    private static final long MAX_COUNTER = 1000

    public void testSharedAccess() {
        long counter = 0

        Actors.defaultPooledActorGroup.resize 2
        def latch = new CountDownLatch(1)

        Actor actor1 = actor {
            loop {
                react {
                    assert it == counter * 2
                    counter += 1
                    it.reply counter.longValue() * 2
                }
            }
        }.start()

        Actor actor2 = actor {
            loop {
                if (counter < MAX_COUNTER) actor1.send counter.longValue() * 2
                else {
                    actor1.stop()
                    stop()
                    latch.countDown()
                }
                react {
                    assert it == counter * 2
                    counter += 1
                }
            }
        }
        actor2.start()


        latch.await()
        Actors.defaultPooledActorGroup.resize(5)
        assertEquals MAX_COUNTER, counter
    }
}
