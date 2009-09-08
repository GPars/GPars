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
