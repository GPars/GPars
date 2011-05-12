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

/**
 * Demonstrates Dataflow variables used to calculate perfect numbers concurrently.
 *
 * @author Vaclav Pech
 * Date: Nov 6, 2009
 */

package groovyx.gpars.samples.dataflow

import groovyx.gpars.dataflow.Dataflow
import groovyx.gpars.dataflow.Dataflows
import static groovyx.gpars.GParsPool.withPool

def isPerfectSequetial(num) {
    def sum = 0
    for (i in (1..num)) {
        if (num % i == 0) sum += i
    }
    return sum == 2 * num
}

def isPerfectWithDF(Integer num) {
    final def flows = new Dataflows()
    final def processors = Math.min(num, Runtime.runtime.availableProcessors() + 1)
    def int chunk = num / processors + 1
    for (i in (1..processors)) {
        final int index = i
        Dataflow.task {
            final int start = chunk * (index - 1) + 1
            final int end = Math.min(num, chunk * index)
            def sum = 0
            if (start <= end) {
                for (currentNum in (start..end)) {
                    if (num % currentNum == 0) sum += currentNum
                }
            }
            flows[index] = sum
        }
    }
    int sum = 0
    for (i in (1..processors)) {
        sum += flows[i]
    }
    println(num + ":" + sum + ":" + (sum == 2 * num))
    return sum == 2 * num
}

withPool {
    assert isPerfectSequetial(6)
    assert !isPerfectWithDF(2)
    assert isPerfectWithDF(6)
    def perfectNumbers = (1..5000).findAll {isPerfectWithDF it}
    println("Perfect numbers: $perfectNumbers")
//    perfectNumbers = (33550300..33550400).findAll {isPerfectWithDF it}
    //    println("Perfect numbers: $perfectNumbers")
}


