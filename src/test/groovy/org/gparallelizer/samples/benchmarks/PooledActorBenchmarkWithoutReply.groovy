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

import org.gparallelizer.actors.pooledActors.PooledActors
import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import java.util.concurrent.CountDownLatch

public class PooledActorBenchmarkWithoutReply implements Benchmark {

    public long perform(final int numberOfIterations) {
        final CountDownLatch latch = new CountDownLatch(1)

        AbstractPooledActor initiator

        final AbstractPooledActor bouncer = PooledActors.actor {
            disableSendingReplies()
            loop {
                react {
                    initiator << '2'
                }
            }
        }.start()

        initiator = PooledActors.actor {
            int iteration = 0
            disableSendingReplies()
            loop {
                if (iteration == numberOfIterations) {
                    latch.countDown()
                    Thread.yield()
                    stop()
                    return
                }
                iteration += 1

                bouncer << '1'
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
