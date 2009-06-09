package org.gparallelizer.samples.benchmarks

import org.gparallelizer.actors.pooledActors.PooledActorGroup
import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import java.util.concurrent.CountDownLatch

final Random random = new Random(System.currentTimeMillis())

System.gc()
Thread.sleep(3000)

final long memory1 = Runtime.runtime.freeMemory()
for (i in 0..100) {
    final CountDownLatch latch = new CountDownLatch(1)
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
        react { }
    }.start()
    latch.await()
}
System.gc()
Thread.sleep(3000)
final long memory2 = Runtime.runtime.freeMemory()
println memory2 - memory1
assert memory2 - memory1 < 1000000