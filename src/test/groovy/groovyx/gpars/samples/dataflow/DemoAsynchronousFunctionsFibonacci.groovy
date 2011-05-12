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

import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.group.DefaultPGroup

/**
 * Demonstrates the way to use DataflowVariables and tasks to create and combine composable asynchronous functions.
 * Inspired by Alex Miller's post (http://tech.puredanger.com/2011/01/19/lamina-channels-and-async-tasks/)
 * and the experiments at https://github.com/ztellman/lamina/wiki/Asynchronous-functions
 *
 * The asyncFun() mechanism is then a generalization of the principle shown here.
 *
 * @author Vaclav Pech
 */

group = new DefaultPGroup(8)

def fib(n) {
    final DataflowVariable result = new DataflowVariable()
    if (n <= 2) result << 1
    else {
        group.task {
            def a = fib(n - 2)
            def b = fib(n - 1)
            a.whenBound {b.whenBound {result << a + b}}
        }
    }
    return result
}

println "Starting the calculation"
final def result = fib(20)
println "Now the calculation is running while we can do something else."

sleep 1000
println "Are we done yet? ${result.bound}"
if (!result.bound) println "Let's do something else then, since the calculation is still running"

sleep 1000
println "Now really, are we done yet? ${result.bound}"

println "OK, I've run out of patience. I'll sit down here and wait for you to finish my calculation!"
println result.val