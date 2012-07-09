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

import groovyx.gpars.GParsPoolUtil

import static groovyx.gpars.GParsPool.withPool

/**
 * Demonstrates the way to use asyncFun() to build composable asynchronous functions.
 * The asyncFun() function allows the user to create an asynchronous variant of a function.
 * Such asynchronous functions accept asynchronous, potentially uncompleted, calculations as parameters (represented by DataflowVariables),
 * perform their own calculation asynchronously using the wrapping thread pool
 * and without blocking the caller they return a DataflowVariable representing a handle to the result of the ongoing asynchronous calculation.
 *
 * Notice the pool may not exist when the asynchronous functions are being created. The functions can pick up a thread pool
 * each time they are invoked.
 *
 * @author Vaclav Pech
 */

Closure sPlus = {Integer a, Integer b ->
    a + b
}

Closure sMultiply = {Integer a, Integer b ->
    sleep 2000
    a * b
}

println "Synchronous result: " + sMultiply(sPlus(10, 30), 100)

Closure aPlus = GParsPoolUtil.asyncFun(sPlus)
Closure aMultiply = GParsPoolUtil.asyncFun(sMultiply)

withPool {
    def result = aMultiply(aPlus(10, 30), 100)

    println "Time to do something else while the calculation is running"
    println "Asynchronous result: " + result.get()
}
