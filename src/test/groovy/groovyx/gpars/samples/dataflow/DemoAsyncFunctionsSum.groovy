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

import groovyx.gpars.dataflow.Promise
import java.util.concurrent.TimeUnit
import static groovyx.gpars.GParsPool.withPool

/**
 * Demonstrates the way to use asyncFun() to build composable asynchronous functions.
 * Inspired by Alex Miller's post (http://tech.puredanger.com/2011/01/19/lamina-channels-and-async-tasks/)
 * and the experiments at https://github.com/ztellman/lamina/wiki/Asynchronous-functions
 *
 * The asyncFun() function allows the user to create an asynchronous variant of a function.
 * Such asynchronous functions accept asynchronous, potentially uncompleted, calculations as parameters (represented by DataflowVariables),
 * perform their own calculation asynchronously using the wrapping thread pool
 * and without blocking the caller they return a DataflowVariable representing a handle to the result of the ongoing asynchronous calculation.
 *
 * @author Vaclav Pech
 */

//Combining an asynchronous summary with the inject (reduce) function

withPool {
    Promise<Integer> result = (0..100000).inject(0, {a, b -> a + b}.asyncFun())
    println "Doing something else while the calculation is running"

    sleep 1000
    println "Are we done yet? ${result.bound}"
    if (!result.bound) println "Let's do something else then, since the calculation is still running"

    sleep 1000
    println "Now really, are we done yet? ${result.bound}"

    println "OK, I've run out of patience. I'll sit down here and wait for you to finish my calculation!"
    println result.get(2, TimeUnit.MINUTES)
}
