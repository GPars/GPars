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

import groovyx.gpars.GParsPool
import groovyx.gpars.dataflow.DataFlowVariable
import jsr166y.forkjoin.RecursiveTask
import static groovyx.gpars.GParsPool.withPool

/**
 * Demonstrates the way to use DataFlowVariables and tasks to create and combine composable asynchronous functions.
 * Inspired by Alex Miller's experiments at https://github.com/ztellman/lamina/wiki/Asynchronous-functions
 *
 * @author Vaclav Pech
 */

def asyncPlus = {a, b ->
    def result = new DataFlowVariable()
    a >> {
        b >> {
            result << a + b
        }
    }
    result
}

def range = 0..100
withPool{
    def result = range.collectParallel{new DataFlowVariable() << it}.parallel.reduce(asyncPlus)
    println "Doing something else while the calculation is running"
    println result.val

    result = range.collectParallel{new DataFlowVariable() << it}.foldParallel(asyncPlus)
    println "Doing something else while the calculation is running"
    println result.val
}

def result = range.collect{new DataFlowVariable() << it}.inject(new DataFlowVariable() << 0, asyncPlus)
println "Doing something else while the calculation is running"
println result.val

def evaluateArguments(pool, args, current, soFarArgs, result, original) {
    if (current==args.size()) {
        pool.submit({->result << original(*soFarArgs)} as RecursiveTask)
    }
    else {
        def currentArgument = args[current]
        if (currentArgument instanceof DataFlowVariable) {
            currentArgument.whenBound {value ->
                evaluateArguments(pool, args, current+1, soFarArgs << value, result, original)
            }
        } else {
            evaluateArguments(pool, args, current+1, soFarArgs << currentArgument, result, original)
        }
    }

}
def asyncFun(final Closure original) {
    final def pool = GParsPool.retrieveCurrentPool()
    return {final Object[] args ->
        final DataFlowVariable result = new DataFlowVariable()
        evaluateArguments(pool, args.clone(), 0, [], result, original)
        result
    }
}

withPool {
//    def sum = ((asyncFun {a, b -> a + b}).call(1, new DataFlowVariable() << 2))
    def sum = (0..100).inject(0, asyncFun{a, b -> a + b})
    println "Doing something else while the calculation is running"
    println sum.val
}
