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

package org.gparallelizer.actors.pooledActors

import java.util.concurrent.atomic.AtomicInteger

public class ReactorTest extends GroovyTestCase{

    public void testMessageProcessing() {
        final def group = new PooledActorGroup()
        final def result1 = new AtomicInteger(0)
        final def result2 = new AtomicInteger(0)
        final def result3 = new AtomicInteger(0)

        final def processor = group.reactor {
            2 * it
        }.start()

        final def a1 = group.actor {
            result1 = processor.sendAndWait(10)
        }
        a1.start()

        final def a2 = group.actor {
            result2 = processor.sendAndWait(20)
        }
        a2.start()

        final def a3 = group.actor {
            result3 = processor.sendAndWait(30)
        }
        a3.start()

        [a1, a2, a3]*.join()
        assertEquals 20, result1
        assertEquals 40, result2
        assertEquals 60, result3

        processor.stop()
        processor.join()
    }
}
