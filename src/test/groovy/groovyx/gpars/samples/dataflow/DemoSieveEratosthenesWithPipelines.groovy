// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2012  The original author or authors
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

package groovyx.gpars.samples.dataflow

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.group.DefaultPGroup

/**
 * Demonstrates concurrent implementation of the Sieve of Eratosthenes using dataflow operator Pipeline DSL.
 * The Pipeline DSL allows for very compact code compared to the other Sieve of Eratosthenes implementations.
 * We're essentially chaining dataflow operators here, but without the need to create and touch the operators explicitly.
 *
 * In principle, the algorithm consists of concurrently run chained filters,
 * each of which detects whether the current number can be divided by a single prime number.
 * (generate nums 1, 2, 3, 4, 5, ...) -&gt; (filter by mod 2) -&gt; (filter by mod 3) -&gt; (filter by mod 5) -&gt; (filter by mod 7) -&gt; (filter by mod 11) -&gt; (caution! Primes falling out here)
 * The chain is built (grows) on the fly, whenever a new prime is found
 */

group = new DefaultPGroup()

final int requestedPrimeNumberCount = 1000

final DataflowQueue initialChannel = new DataflowQueue()

/**
 * Generating candidate numbers
 */
group.task {
    (2..10000).each {
        initialChannel << it
    }
}

/**
 * Consume Sieve output and add additional filters for all found primes
 */
group.task {
    def primesChannel = initialChannel
    requestedPrimeNumberCount.times {
        int prime = primesChannel.val
        println "Found: $prime"
        primesChannel = primesChannel.filter { it % prime != 0 }
    }
}.join()
