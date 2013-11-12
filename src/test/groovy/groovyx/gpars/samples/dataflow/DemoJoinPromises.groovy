// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2013  The original author or authors
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

import static groovyx.gpars.dataflow.Dataflow.task
import static groovyx.gpars.dataflow.Dataflow.whenAllBound

/**
 * Demonstrates the thenForkAndJoin() capability of promises. The thenForkAndJoin() method triggers multiple 'then' handlers,
 * once a promise they wait for has been bound. The method returns a promise eventually containing a list of results of all the parallel 'then' handlers.
 * in a list to the next
 */
task {
    println "task 1"
}.then {
    whenAllBound(
            task {
                println "task 2"
                2
            },
            task {
                println "task 3"
                3
            }) { a, b -> [a, b] }
}.then { println it }.join()


println "---------------------- The code above should be equivalent to the code below"

task {
    println "task 1"
}.thenForkAndJoin(
        {
            println "task 2"
            2
        },
        {
            println "task 3"
            3
        }).then({ println it }).join()