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
import static groovyx.gpars.dataflow.Dataflow.whenAllBound

/**
 * Uses the Dataflow.whenAllBound() and Promise.then() (aka rightShift) methods to wire together multiple asynchronous functions.
 */

withPool {
    Closure download = { String url ->
        sleep 3000  //Simulate a web read
        'web content'
    }.asyncFun()

    Closure loadFile = { String fileName ->
        'file content'  //simulate a local file read
    }.asyncFun()

    Closure hash = { s -> s.hashCode() }

    Closure compare = { int first, int second ->
        first == second
    }

    Closure errorHandler = { println "Error detected: $it" }

    def all = whenAllBound([
            download('http://www.gpars.org') >> hash,
            loadFile('/coolStuff/gpars/website/index.html') >> hash
    ], compare) then({ println it }, errorHandler)
    all.join()  //optionally block until the calculation is all done
}