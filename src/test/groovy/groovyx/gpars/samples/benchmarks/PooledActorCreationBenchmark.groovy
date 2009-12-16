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
import groovyx.gpars.actor.Actors

public class PooledActorCreationBenchmark implements Benchmark {

    public long perform(final int numberOfIterations) {
        final AbstractPooledActor initiator = Actors.actor {
            int iteration = 0
            loop {
                if (iteration == numberOfIterations) {
                    Thread.yield()
                    stop()
                    return
                }
                iteration += 1

                new PooledBouncer().start() << '1'
                react { }
            }
        }

        final long t1 = System.currentTimeMillis()
        initiator.join()
        final long t2 = System.currentTimeMillis()

        return (t2 - t1)
    }
}

class PooledBouncer extends AbstractPooledActor {
    void act() {
        react {
            reply '2'
        }
    }
}
