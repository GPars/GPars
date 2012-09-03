// GPars - Groovy Parallel Systems
//
// Copyright © 2008--2011  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.benchmark

import groovy.time.TimeCategory
import groovyx.gpars.GParsPoolUtil
import groovyx.gpars.ParallelEnhancer
import jsr166y.ForkJoinPool
import groovyx.gpars.extra166y.Ops.Reducer
import groovyx.gpars.extra166y.ParallelArray
import static groovyx.gpars.GParsPool.withExistingPool
import static groovyx.gpars.GParsPool.withPool

long start

class MyNumber {
    private Random random = new Random()
    long value

    public MyNumber plus(MyNumber other) {
        for (i in (1..50000)) random.nextInt()
        new MyNumber(value: this.value + other.value)
    }
}
//def nums = (1L..10000000L)
List nums = (1L..10000000L).collect {it}
//Long[] nums = (1L..10000000L).collect{it}
println nums.class
//def nums = (1L..10000000L).collect {new TimeDuration(1, 2, it as int, it as int)}
//def nums = (1L..100000L).collect {new MyNumber(value : it)}
//def nums = (1L..10000000L).collect {5}
//def nums = (1L..1000000L).collect {BigDecimal.valueOf(it)}

final def pool = new ForkJoinPool()

withExistingPool(pool) {
    println ""
    println "Warming up"
    println nums.sum()
    println GParsPoolUtil.sumParallel(nums)
    println GParsPoolUtil.sumParallel(nums)
    println GParsPoolUtil.getParallel(nums).sum()
    println GParsPoolUtil.getParallel(nums).sum()
    println ParallelArray.createFromCopy(nums.toArray(new Long[nums.size()]), pool).reduce({a, b -> a + b} as Reducer, null)
//    println ParallelArray.createFromCopy(nums, pool).reduce({a, b -> a + b} as Reducer, null)
}

withPool {
    sleep 2000
    println ""
    println "serially summing numbers inside a withPool"
    start = System.currentTimeMillis()
    println nums.sum()
    println "time: ${System.currentTimeMillis() - start}ms"
}

use(TimeCategory) {
    sleep 2000
    println ""
    println "serially summing numbers inside a TimeCategory block"
    start = System.currentTimeMillis()
    println nums.sum()
    println "time: ${System.currentTimeMillis() - start}ms"
}

sleep 2000
println ""
println "serially summing numbers outside a withPool"
start = System.currentTimeMillis()
println nums.sum()
println "time: ${System.currentTimeMillis() - start}ms"

withExistingPool(pool) {
    sleep 2000
    println ""
    println "paralell summing numbers inside a withPool"
    start = System.currentTimeMillis()
    println GParsPoolUtil.sumParallel(nums)
    println "time: ${System.currentTimeMillis() - start}ms"
}

withPool {
    sleep 2000
    println ""
    println GParsPoolUtil.getParallel(nums).sum()
    println "paralell summing numbers inside a withPool using PA"
    start = System.currentTimeMillis()
    println GParsPoolUtil.getParallel(nums).sum()
    println "time: ${System.currentTimeMillis() - start}ms"
}

withPool {
    sleep 2000
    println ""
    println "paralell summing numbers inside a withPool using PA ignoring PA build time"
    final def pnums = GParsPoolUtil.getParallel(nums)
    start = System.currentTimeMillis()
    println pnums.sum()
    println "time: ${System.currentTimeMillis() - start}ms"
}

println ""
sleep 2000
println "paralell summing numbers inside a withPool using PA directly"
start = System.currentTimeMillis()
def pnums = ParallelArray.createFromCopy(nums.toArray(new Long[nums.size()]), pool)
//def pnums = ParallelArray.createFromCopy(nums, pool)
println pnums.reduce({a, b -> a + b} as Reducer, null)
println "time: ${System.currentTimeMillis() - start}ms"

sleep 2000
println ""
println "paralell summing numbers using an enhancer"
start = System.currentTimeMillis()
ParallelEnhancer.enhanceInstance(nums)
println nums.sumParallel()
println "time: ${System.currentTimeMillis() - start}ms"
