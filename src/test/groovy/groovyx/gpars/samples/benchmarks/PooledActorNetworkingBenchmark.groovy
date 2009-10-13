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

import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.impl.AbstractPooledActor
import java.util.concurrent.CountDownLatch

public class PooledActorNetworkingBenchmark implements Benchmark {

    public long perform(final int numberOfIterations) {
        final long t1 = System.currentTimeMillis()
        final PooledNetworkingMaster master = new PooledNetworkingMaster(numActors: 10, iterations: numberOfIterations)
        master.start()
        master.waitUntilDone()
        final long t2 = System.currentTimeMillis()
        master.stopAll()

        return (t2 - t1)
    }
}

class PooledWorkerActor extends AbstractPooledActor {
    void act() {
        loop {
            react {
                reply '2'
            }
        }
    }
}

final class PooledNetworkingMaster extends AbstractPooledActor {

    int iterations = 1
    int numActors = 1

    private List<Actor> workers

    private CountDownLatch startupLatch = new CountDownLatch(1)
    private CountDownLatch doneLatch

    private void beginSorting() {
        workers = createWorkers()
        int cnt = sendTasksToWorkers()
        doneLatch = new CountDownLatch(cnt)
        startupLatch.countDown()
    }

    private List createWorkers() {
        return (1..numActors).collect {new PooledWorkerActor().start()}
    }

    private int sendTasksToWorkers() {
        int cnt = 0
        for (i in 1..iterations) {
            workers[cnt % numActors] << '1'
            cnt += 1
        }
        return cnt
    }

    public void waitUntilDone() {
        startupLatch.await()
        doneLatch.await()
    }

    void act() {
        beginSorting()
        loop {
            react {
                doneLatch.countDown()
            }
        }
    }

    public void stopAll() {
        workers.each {it.stop()}
        stop()
        workers = null
    }
}
