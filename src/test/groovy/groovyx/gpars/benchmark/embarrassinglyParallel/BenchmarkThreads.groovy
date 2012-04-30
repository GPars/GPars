// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
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

package groovyx.gpars.benchmark.embarrassinglyParallel

import java.util.concurrent.ArrayBlockingQueue

def execute(int actorCount) {
    final long n = 100000000l // 10 times fewer due to speed issues.
    final double delta = 1.0d / n
    final long sliceSize = n / actorCount
    final long startTimeNanos = System.nanoTime()
    final List computors = []
//    final DataflowQueue stream = new DataflowQueue()
    final Queue queue = new ArrayBlockingQueue(actorCount)
    final accumulator = new Thread({
        double sum = 0.0d
        for (c in computors) {sum += queue.take()}
        final double pi = 4.0d * sum * delta
        final double elapseTime = (System.nanoTime() - startTimeNanos) / 1e9
        println("==== Groovy GPars ActorScript pi = " + pi)
        println("==== Groovy GPars ActorScript iteration count = " + n)
        println("==== Groovy GPars ActorScript elapse = " + elapseTime)
        println("==== Groovy GPars ActorScript actor count = " + actorCount)
    })

    (0..<actorCount).each {long index ->
        final long start = 1l + index * sliceSize
        final long end = (index + 1l) * sliceSize
        computors.add(
                Thread.start {
                    double sum = 0.0d
                    for (long i = start; i <= end; ++i) {
                        final double x = (i - 0.5d) * delta
                        sum += 1.0d / (1.0d + x * x)
                    }
                    queue.offer(sum)
                }
        )
    }
    accumulator.start()
    accumulator.join()
}

execute(1)
println()
execute(2)
println()
execute(4)
println()
execute(8)
println()
execute(32)

