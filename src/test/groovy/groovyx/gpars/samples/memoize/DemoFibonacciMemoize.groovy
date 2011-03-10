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

package groovyx.gpars.samples.memoize

import groovyx.gpars.GParsPool

/**
 * Demonstrates closure result caching through the gmemoize mechanism
 * Without caching the raw algorithm would end-up taking very long to finish given the inherent
 * exponential time complexity of it.
 * Thanks to caching introduced through gmemoize(), each Fibonacci number has to be calculated only once
 * and so we turned the exponential complexity algorithm into a sequential one. With a single extra method call.
 */

GParsPool.withPool {
    Closure fib
    fib = {n -> n > 1 ? fib(n - 2) + fib(n - 1) : n}.gmemoizeAtMost(2)  //try to remove the gmemoize() method call to get the original slow exponential complexity behavior
    println "Fib for 0: ${fib(0)}"
    println "Fib for 1: ${fib(1)}"
    println "Fib for 2: ${fib(2)}"
    println "Fib for 3: ${fib(3)}"
    println "Fib for 4: ${fib(4)}"
    println "Fib for 10: ${fib(10)}"
    println "Fib for 40: ${fib(40)}"
}