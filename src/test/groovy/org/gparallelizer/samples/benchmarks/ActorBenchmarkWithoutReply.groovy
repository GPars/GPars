package org.gparallelizer.samples.benchmarks

import java.util.concurrent.CountDownLatch
import org.gparallelizer.actors.Actor
import org.gparallelizer.actors.Actors

public class ActorBenchmarkWithoutReply implements Benchmark {

    public long perform(final int numberOfIterations) {
        final CountDownLatch latch = new CountDownLatch(1)
        private int iteration = 0

        final Actor initiator

        final Actor bouncer = Actors.actor {
            disableSendingReplies()
            receive()
            initiator << '2'
        }.start()

        initiator = Actors.actor {
            if (iteration == numberOfIterations) {
                disableSendingReplies()
                latch.countDown()
                Thread.yield()
                stop()
                return
            }
            iteration += 1

            bouncer << '1'
            receive()
        }

        final long t1 = System.currentTimeMillis()
        initiator.start()
        latch.await()
        final long t2 = System.currentTimeMillis()
        bouncer.stop()

        return (t2 - t1)
    }
}