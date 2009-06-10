package org.gparallelizer.samples.benchmarks

import org.gparallelizer.actors.pooledActors.PooledActors
import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import java.util.concurrent.CountDownLatch

public class PooledActorBenchmark implements Benchmark {

    public long perform(final int numberOfIterations) {
        final CountDownLatch latch = new CountDownLatch(1)

        final AbstractPooledActor bouncer = PooledActors.actor {
            loop {
                react {
                    reply '2'
                }
            }
        }.start()

        final AbstractPooledActor initiator = PooledActors.actor {
                int iteration = 0
                loop {
                    if (iteration >= numberOfIterations) {
                        latch.countDown()
                        Thread.yield()
                        stop()
                        return
                    }
                    iteration += 1

                    bouncer <<  '1'
                    react { }
                }
        }

        final long t1 = System.currentTimeMillis()
        initiator.start()
        latch.await()
        final long t2 = System.currentTimeMillis()
        bouncer.stop()

        return (t2 - t1)
    }
}