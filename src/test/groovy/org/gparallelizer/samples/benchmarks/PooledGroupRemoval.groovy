package org.gparallelizer.samples.benchmarks

import org.gparallelizer.actors.pooledActors.PooledActorGroup
import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import java.util.concurrent.CountDownLatch

final Random random = new Random(System.currentTimeMillis())

cleanMemory()

final long memory1 = Runtime.runtime.freeMemory()
println 'Threads at start: ' + Thread.threads.length
for (i in 0..10000) {
    final CountDownLatch latch = new CountDownLatch(2)
    final PooledActorGroup group = new PooledActorGroup(i % 2 == 0, Math.max(1, random.nextInt(20)))
    final AbstractPooledActor actor = group.actor {
        loop {
            react {
                reply it
                stop()
                latch.countDown()
            }
        }
    }.start()

    group.actor {
        actor << 'Message'
        react {
            latch.countDown()
        }
    }.start()
    latch.await()
    group.shutdown()
}
cleanMemory()
println 'Threads at finish: ' + Thread.threads.length
final long memory2 = Runtime.runtime.freeMemory()
println ((memory2 - memory1)/1000000)
assert memory2 - memory1 < 3000000

private def cleanMemory() {
    for (i in 0..3000) {
        final def int[] ints = new int[50000]
        if (ints[0] > 10) ints[10] = ints[20]
    }
    System.gc()
    Thread.sleep(3000)
}