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

import static groovyx.gpars.dataflow.Dataflow.operator
import static groovyx.gpars.dataflow.Dataflow.task as go

/**
 * Demonstrates a concurrent implementation of the Sieve of Eratosthenes using dataflow tasks mimicking closely the example
 * given in the Google's Go programming language, but overcoming the co-routine shortage on JVM.
 *
 * In principle, the algorithm consists of concurrently run chained filters,
 * each of which detects whether the current number can be divided by a single prime number.
 * (generate nums 1, 2, 3, 4, 5, ...) -&gt; (filter by mod 2) -&gt; (filter by mod 3) -&gt; (filter by mod 5) -&gt; (filter by mod 7) -&gt; (filter by mod 11) -&gt; (caution! Primes falling out here)
 * The chain is built (grows) on the fly, whenever a new prime is found
 */

// Send the sequence 2, 3, 4, ... to channel 'ch'.

def generate(ch) {
    {->
        for (i in (2..100000)) {
            ch << i
        }
    }
}

// Copy the values from channel 'in' to channel 'out',
// removing those divisible by 'prime'.

def filter(inChannel, outChannel, int prime) {
    operator([inputs: [inChannel], outputs: [outChannel]]) { number ->
        if (number % prime != 0) {
            bindOutput number
        }
    }
}

// The prime sieve: Daisy-chain Filter processes.

def main() {
    def ch = new DataflowQueue()
    go generate(ch)
    for (i in (1..10000)) {
        int prime = ch.val
        println prime
        def ch1 = new DataflowQueue()
        filter(ch, ch1, prime)
        ch = ch1
    }
}

//Now run it all
main()


