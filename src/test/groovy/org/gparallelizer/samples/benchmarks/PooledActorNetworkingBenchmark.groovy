package org.gparallelizer.samples.benchmarks

import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import java.util.concurrent.CountDownLatch
import org.gparallelizer.actors.pooledActors.PooledActor


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

    private List<PooledActor> workers

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