package org.gparallelizer.samples.benchmarks

import java.util.concurrent.CountDownLatch
import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import org.gparallelizer.actors.pooledActors.PooledActors

public class PooledActorCreationBenchmark implements Benchmark {

    public long perform(final int numberOfIterations) {
        final CountDownLatch latch = new CountDownLatch(1)

        final AbstractPooledActor initiator = PooledActors.actor {
            int iteration = 0
            loop {
                if (iteration == numberOfIterations) {
                    latch.countDown()
                    Thread.yield()
                    stop()
                    return
                }
                iteration += 1

                new PooledBouncer().start() << 1
                react { }
            }
        }

        final long t1 = System.currentTimeMillis()
        initiator.start()
        latch.await()
        final long t2 = System.currentTimeMillis()

        return (t2 - t1)
    }
}

class PooledBouncer extends AbstractPooledActor {
    void act() {
        react {
            reply 2
        }
    }
}