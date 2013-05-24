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

package groovyx.gpars.samples.dataflow

import groovyx.gpars.dataflow.DataflowVariable

import static groovyx.gpars.GParsPool.withPool

/**
 * Demonstrates the way to use DataflowVariables and tasks to create and combine composable asynchronous functions.
 * Inspired by Alex Miller's post (http://tech.puredanger.com/2011/01/19/lamina-channels-and-async-tasks/)
 * and the experiments at https://github.com/ztellman/lamina/wiki/Asynchronous-functions
 *
 * The asyncFun() mechanism is then a generalization of the principle shown here.
 * @author Vaclav Pech
 */

def asyncPlus = { a, b ->
    def result = new DataflowVariable()
    a >> {
        b >> {
            result << a + b
        }
    }
    result
}

def range = 0..100000
withPool {
    def result = range.collectParallel { new DataflowVariable() << it }.parallel.reduce(asyncPlus)
    println "Doing something else while the calculation is running"
    println result.val

    result = range.collectParallel { new DataflowVariable() << it }.injectParallel(asyncPlus)
    println "Doing something else while the calculation is running"
    println result.val
}

def result = range.collect { new DataflowVariable() << it }.inject(new DataflowVariable() << 0, asyncPlus)
println "Doing something else while the calculation is running"
println result.val
