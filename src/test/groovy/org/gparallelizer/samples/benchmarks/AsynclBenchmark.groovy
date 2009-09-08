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

import org.gparallelizer.Asynchronizer
import org.gparallelizer.Parallelizer

List items = []
for (i in 1..100000) {items << i}

final def numOfIterations = 1..10
final def numOfWarmupIterations = 1..10

meassureSequential(numOfWarmupIterations, items)
final long time = meassureSequential(numOfIterations, items)
println "Sequential $time"

meassureAsynchronizer(numOfWarmupIterations, items)
time = meassureAsynchronizer(numOfIterations, items)
println "Asynchronizer $time"

meassureParallelizer(numOfWarmupIterations, items)
time = meassureParallelizer(numOfIterations, items)
println "Parallelizer $time"

long meassureSequential(iterations, List list) {
    final long t1 = System.currentTimeMillis()
    for (i in iterations) {
        int result
        list.each {result = it}
        def elements = list.collect {it}
        result = elements[-1]
    }
    final long t2 = System.currentTimeMillis()
    return t2 - t1
}

long meassureAsynchronizer(iterations, List list) {
    final long t1 = System.currentTimeMillis()
    Asynchronizer.withAsynchronizer(30) {
        for (i in iterations) {
            int result
            list.eachAsync {result = it}
            def elements = list.collectAsync {it}
            result = elements[-1]
        }
    }
    final long t2 = System.currentTimeMillis()
    return t2 - t1
}

long meassureParallelizer(iterations, List list) {
    final long t1 = System.currentTimeMillis()
    Parallelizer.withParallelizer(30) {
        for (i in iterations) {
            int result
            list.eachAsync {result = it}
            def elements = list.collectAsync {it}
            result = elements[-1]
        }
    }
    final long t2 = System.currentTimeMillis()
    return t2 - t1
}
