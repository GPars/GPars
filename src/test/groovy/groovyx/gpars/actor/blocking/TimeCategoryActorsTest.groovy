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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.codehaus.groovy.runtime.TimeCategory

/**
 *
 * @author Vaclav Pech
 * Date: Jan 16, 2009
 */

public class TimeCategoryActorsTest extends GroovyTestCase {
    public void testReceive() {
        volatile def result = ''
        final CountDownLatch latch = new CountDownLatch(1)

        final Actor actor = Actors.actor {
            use(TimeCategory) {
                result = receive(3.seconds)
            }
            latch.countDown()
        }

        latch.await(90, TimeUnit.SECONDS)
        assertNull(result)
    }

    public void testTimeCategoryNotAvailable() {
        volatile def exceptions = 0
        final CountDownLatch latch = new CountDownLatch(1)

        def actor = Actors.actor {
            try {
                receive(1.second) {}
            } catch (MissingPropertyException ignore) {exceptions++ }
            loop {
                try {
                    try {
                        receive(1.minute) {}
                    } catch (MissingPropertyException ignore) {exceptions++ }
                    stop()
                } finally {
                    latch.countDown()
                }
            }
        }

        actor.join ()
        latch.await()
        assertEquals 2, exceptions
    }

    public void testReceiveWithHandler() {
        volatile def result = ''
        final CountDownLatch latch = new CountDownLatch(1)

        final Actor actor = Actors.actor {
            use(TimeCategory) {
                receive(2.seconds) {
                    result = it
                }
                latch.countDown()
            }
        }

        latch.await(90, TimeUnit.SECONDS)
        assertNull(result)
    }
}
