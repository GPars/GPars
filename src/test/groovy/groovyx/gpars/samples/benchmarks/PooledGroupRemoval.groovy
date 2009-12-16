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

package groovyx.gpars.samples.benchmarks

import groovyx.gpars.actor.AbstractPooledActor
import groovyx.gpars.actor.PooledActorGroup
import java.util.concurrent.CountDownLatch

final Random random = new Random(System.currentTimeMillis())

cleanMemory()

final long memory1 = Runtime.runtime.freeMemory()
println 'Threads at start: ' + Thread.threads.length
for (i in 0..10000) {
    final CountDownLatch latch = new CountDownLatch(2)
    final PooledActorGroup group = new PooledActorGroup(Math.max(1, random.nextInt(20)), i % 2 == 0)
    final AbstractPooledActor actor = group.actor {
        loop {
            react {
                reply it
                stop()
                latch.countDown()
            }
        }
    }

    group.actor {
        actor << 'Message'
        react {
            latch.countDown()
        }
    }
    latch.await()
    group.shutdown()
}
cleanMemory()
println 'Threads at finish: ' + Thread.threads.length
final long memory2 = Runtime.runtime.freeMemory()
println((memory2 - memory1) / 1000000)
assert memory2 - memory1 < 3000000

private def cleanMemory() {
    for (i in 0..3000) {
        final def int[] ints = new int[50000]
        if (ints[0] > 10) ints[10] = ints[20]
    }
    System.gc()
    Thread.sleep(3000)
}
