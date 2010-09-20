// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
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

package groovyx.gpars.samples.actors

import groovyx.gpars.actor.DynamicDispatchActor
import groovyx.gpars.group.NonDaemonPGroup

/**
 * Demonstrates concurrent implementation of the Sieve of Eratosthenes using actors
 *
 * In principle, the algorithm consists of a concurrently run chained filters,
 * each of which detects whether the current number can be divided by a single prime number.
 * (generate nums 1, 2, 3, 4, 5, ...) -> (filter by mod 2) -> (filter by mod 3) -> (filter by mod 5) -> (filter by mod 7) -> (filter by mod 11) ->
 * The chain is built (grows) on the fly, whenever a new prime is found
 */

/**
 * We need a resizeable thread pool, since tasks consume threads while waiting blocked for values at DataFlowStream.val
 */
class FooGroup {
    static def group = new NonDaemonPGroup(8)
}

int requestedPrimeNumberBoundary = 1000

final def firstFilter = new FilterActor(2).start()

/**
 * Generating candidate numbers
 */
FooGroup.group.task {
    (2..requestedPrimeNumberBoundary).each {
        firstFilter it
    }
    firstFilter null
}

/**
 * Chain a new filter for a particular prime number to the end of the Sieve
 * @param inChannel The current end channel to consume
 * @param prime The prime number to divide future prime candidates with
 * @return A new channel ending the whole chain
 */

/**
 * Consume Sieve output and add additional filters for all found primes
 */

final class FilterActor extends DynamicDispatchActor {
    private final int myPrime
    private def follower

    def FilterActor(final myPrime) {
        this.myPrime = myPrime;
        this.parallelGroup = FooGroup.group
    }

    def onMessage(int value) {
        if (value % myPrime != 0) {
            if (follower) follower value
            else {
                follower = new FilterActor(value).start()
                println "Found $value"
            }
        }
    }

    def onMessage(def poisson) {
        follower?.send poisson
        stop()
        if (!follower) this.parallelGroup.shutdown()
    }
}