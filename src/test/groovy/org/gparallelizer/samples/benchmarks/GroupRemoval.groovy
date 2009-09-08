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

package org.gparallelizer.samples.benchmarks

import java.util.concurrent.CountDownLatch
import org.gparallelizer.actors.AbstractThreadActorGroup
import org.gparallelizer.actors.Actor
import org.gparallelizer.actors.ThreadActorGroup

final Random random = new Random(System.currentTimeMillis())

cleanMemory()

final long memory1 = Runtime.runtime.freeMemory()
println 'Threads at start: ' + Thread.threads.length
for (i in 0..10000) {
    final CountDownLatch latch = new CountDownLatch(1)
    final AbstractThreadActorGroup group = new ThreadActorGroup(Math.max(1, random.nextInt(20)), i % 2 == 0)
    final Actor actor = group.oneShotActor {
        receive {
            reply it
        }
    }.start()

    group.oneShotActor {
        actor << 'Message'
        receive {
            latch.countDown()
        }
    }.start()
    latch.await()
    group.shutdown()
}
cleanMemory()
println 'Threads at finish: ' + Thread.threads.length
final long memory2 = Runtime.runtime.freeMemory()
println memory2 - memory1
assert memory2 - memory1 < 1000000

private def cleanMemory() {
    println 'Cleaning memory'
    for (i in 0..5000) {
        final def int[] ints = new int[50000]
        if (ints[0] > 10) ints[10] = ints[20]
    }
    System.gc()
    Thread.sleep(3000)
}
