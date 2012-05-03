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

import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.group.DefaultPGroup

def execute(int actorCount) {
    final long n = 100000000l // 10 times fewer due to speed issues.
    final double delta = 1.0d / n
    final long sliceSize = n / actorCount
    final long startTimeNanos = System.nanoTime()
    final List computors = [].asSynchronized()
    final group = new DefaultPGroup(actorCount) // Interesting (!) behaviour with the +1 missing.
    def startAccumulator = new DataflowVariable<Boolean>()
    final accumulator = group.actor {
        double sum = 0.0d
        int counter = 0
        startAccumulator.val
        loop {
            if (counter < computors.size()) {
                counter++
                react { sum += it }
            } else {
                final double pi = 4.0d * sum * delta
                final double elapseTime = (System.nanoTime() - startTimeNanos) / 1e9
                println("==== Groovy GPars ActorScript pi = " + pi)
                println("==== Groovy GPars ActorScript iteration count = " + n)
                println("==== Groovy GPars ActorScript elapse = " + elapseTime)
                println("==== Groovy GPars ActorScript actor count = " + actorCount)
                stop()
            }
        }
    }
    (0..<actorCount).each {long index ->
        final long start = 1l + index * sliceSize
        final long end = (index + 1l) * sliceSize
        computors.add(
                group.actor {
                    double sum = 0.0d
                    for (long i = start; i <= end; ++i) {
                        final double x = (i - 0.5d) * delta
                        sum += 1.0d / (1.0d + x * x)
                    }
                    accumulator << sum
                }
        )

    }
    startAccumulator.bind(true)
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

