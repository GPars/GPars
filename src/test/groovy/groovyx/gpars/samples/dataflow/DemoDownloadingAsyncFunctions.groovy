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
 * @author Vaclav Pech
 */

withPool {
    Closure download = {String url ->
        url.toURL().text
    }

    Closure scanFor = {String word, String text ->
        text.findAll(word).size()
    }

    Closure lower = {s -> s.toLowerCase()}

    println scanFor('groovy', lower(download('http://www.infoq.com')))  //synchronous processing

    //asynchronous processing converting the synchronous functions into asynchronous ones in-place
    def result = scanFor.asyncFun()('groovy', lower.asyncFun()(download.asyncFun()('http://www.infoq.com')))
    println 'Allowed to do something else now'
    println result.get()
}

//now we'll instead make the functions asynchronous right-away
withPool {
    Closure download = {String url ->
        url.toURL().text
    }.asyncFun()

    Closure scanFor = {String word, String text ->
        text.findAll(word).size()
    }.asyncFun()

    Closure lower = {s -> s.toLowerCase()}.asyncFun()

    //asynchronous processing
    def result = scanFor('groovy', lower(download('http://www.infoq.com')))
    println 'Allowed to do something else now'
    println result.get()
}
