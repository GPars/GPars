package org.gparallelizer.samples.benchmarks

import java.util.concurrent.CountDownLatch
import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import org.gparallelizer.actors.Actor
import org.gparallelizer.actors.Actors
import org.gparallelizer.actors.DefaultActor


public class ActorCreationBenchmark implements Benchmark {

    public long perform(final int numberOfIterations) {
        final CountDownLatch latch = new CountDownLatch(1)
        private int iteration = 0

        final Actor initiator = Actors.actor {
            if (iteration == numberOfIterations) {
                latch.countDown()
                Thread.yield()
                stop()
                return
            }
            iteration += 1

            new Bouncer().start() << '1'
            receive()
        }

        final long t1 = System.currentTimeMillis()
        initiator.start()
        latch.await()
        final long t2 = System.currentTimeMillis()

        return (t2 - t1)
    }
}

class Bouncer extends DefaultActor {
    void act() {
        receive()
        reply '2'
        stop()
    }
}