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

package groovyx.gpars.samples.dataflow.thenChaining

import static groovyx.gpars.GParsPool.withPool

/**
 * Asynchronous function composition
 */

withPool {
    Closure download = {String url ->
        sleep 3000
        'web content'
    }.asyncFun()

    Closure loadFile = {String fileName ->
        'file content'
    }.asyncFun()

    Closure hash = {s -> s.hashCode()}.asyncFun()

    Closure compare = {int first, int second ->
        first == second
    }.asyncFun()

    def result = compare(hash(download('http://www.gpars.org')), hash(loadFile('/coolStuff/gpars/website/index.html')))
    println "The result of comparison: " + result.get()
}