package org.gparallelizer.samples.benchmarks

import org.gparallelizer.actors.ActorGroup
import org.gparallelizer.actors.Actor
import java.util.concurrent.CountDownLatch

cleanMemory()

final long memory1 = Runtime.runtime.freeMemory()
for (i in 0..1000) {
    final CountDownLatch latch = new CountDownLatch(1)
    final ActorGroup group = new ActorGroup(i % 2 == 0)
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
}
cleanMemory()
final long memory2 = Runtime.runtime.freeMemory()
println memory2 - memory1
assert memory2 - memory1 < 1000000

private def cleanMemory() {
    final def block = []
    for (i in 0..200) {
        final def int[] ints = new int[50000]
        if (ints[0] > 10) ints[10] = ints[20]
        block << ints
    }
    block = null
    System.gc()
    Thread.sleep(10000)
}