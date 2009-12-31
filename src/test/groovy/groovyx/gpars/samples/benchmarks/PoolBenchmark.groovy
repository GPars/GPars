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

import groovyx.gpars.scheduler.DefaultPool
import groovyx.gpars.scheduler.FJPool
import groovyx.gpars.scheduler.Pool
import groovyx.gpars.scheduler.ResizeableFJPool
import groovyx.gpars.scheduler.ResizeablePool
import groovyx.gpars.scheduler.Scheduler
import java.util.concurrent.CyclicBarrier

List items = []
for (i in 1..1000) {items << {i + it}}

final def numOfIterations = 1..100
final def numOfWarmupIterations = 1..100


meassureSequential(numOfWarmupIterations, items)
final long time = meassureSequential(numOfIterations, items)
println "Sequential $time"

meassurePool(numOfWarmupIterations, items, new DefaultPool(true, 3))
time = meassurePool(numOfIterations, items, new DefaultPool(true, 3))
println "Default Pool $time"

meassurePool(numOfWarmupIterations, items, new ResizeablePool(true, 3))
time = meassurePool(numOfIterations, items, new ResizeablePool(true, 3))
println "Resizeable Pool $time"

meassurePool(numOfWarmupIterations, items, new FJPool(3))
time = meassurePool(numOfIterations, items, new FJPool(3))
println "FJ Pool $time"

meassurePool(numOfWarmupIterations, items, new ResizeableFJPool(3))
time = meassurePool(numOfIterations, items, new ResizeableFJPool(3))
println "Resizeable FJ Pool $time"

meassurePool(numOfWarmupIterations, items, new Scheduler(3))
time = meassurePool(numOfIterations, items, new Scheduler(3))
println "Custom Scheduler $time"

long measureSequential(iterations, List tasks) {
    final long t1 = System.currentTimeMillis()
    for (i in iterations) {
        for (task in tasks) {
            int result = task.call(10)
            if (result < 0) println result
        }
    }
    final long t2 = System.currentTimeMillis()
    return t2 - t1
}

long measurePool(iterations, List tasks, Pool pool) {
    final long t1 = System.currentTimeMillis()
    for (i in iterations) {
        for (task in tasks) {
            pool.execute {
                int result = task.call(10)
                if (result < 0) println result
            }
        }
        pause(pool)
    }
    pool.shutdown()
    final long t2 = System.currentTimeMillis()
    return t2 - t1
}

private def pause(Pool pool) {
    final CyclicBarrier barrier = new CyclicBarrier(2)
    pool.execute { barrier.await() }
    barrier.await()
}
