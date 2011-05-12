// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.samples.dataflow

import static groovyx.gpars.GParsPool.withPool

/**
 * Demonstrates the way to use asyncFun() to build composable asynchronous functions.
 * The asyncFun() function allows the user to create an asynchronous variant of a function.
 * Such asynchronous functions accept asynchronous, potentially uncompleted, calculations as parameters (represented by DataflowVariables),
 * perform their own calculation asynchronously using the wrapping thread pool
 * and without blocking the caller they return a DataflowVariable representing a handle to the result of the ongoing asynchronous calculation.
 *
 * This particular example demonstrates the ability of asynchronous functions to compose result promises.
 * If an asynchronous function (e.f. the 'distance' function in the example) in its body calls another asynchronous function
 * (e.g. 'plus') and returns the the promise of the invoked function, the inner function's (plus) result promise will compose with the outer function's (distance)
 * result promise. The inner function (plus) will now bind its result to the outer function's (distance) promise, once the inner function (plus) finishes its calculation.
 *
 * This ability of promises to compose allows functions to cease their calculation without blocking a thread not only when waiting for parameters,
 * but also whenever they call another asynchronous function anywhere in their body.
 * @author Vaclav Pech
 */

withPool {
    Closure plus = {Integer a, Integer b ->
        sleep 3000
        println 'Adding numbers'
        a + b
    }.asyncFun()

    Closure multiply = {Integer a, Integer b ->
        sleep 2000
        a * b
    }.asyncFun()

    Closure measureTime = {->
        sleep 3000
        4
    }.asyncFun()

    Closure distance = {Integer initialDistance, Integer velocity, Integer time ->
        plus(initialDistance, multiply(velocity, time))
    }.asyncFun()

    Closure chattyDistance = {Integer initialDistance, Integer velocity, Integer time ->
        println 'All parameters are now ready - starting'
        println 'About to call another asynchronous function'
        def innerResultPromise = plus(initialDistance, multiply(velocity, time))
        println 'Returning the promise for the inner calculation as my own result'
        return innerResultPromise
    }.asyncFun()

    println "Distance = " + distance(100, 20, measureTime()).get() + ' m'
    println "ChattyDistance = " + chattyDistance(100, 20, measureTime()).get() + ' m'
}
