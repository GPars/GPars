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


def evaluateArguments(pool, args, current, soFarArgs, result, original, pooledThreadFlag) {
    if (current==args.size()) {
        if (pooledThreadFlag) result << original(*soFarArgs)
        else {
            pool.submit({->result << original(*soFarArgs)} as RecursiveTask)
        }
    }
    else {
        def currentArgument = args[current]
        if (currentArgument instanceof DataFlowVariable) {
            currentArgument.whenBound {value ->
                evaluateArguments(pool, args, current+1, soFarArgs << value, result, original, true)
            }
        } else {
            evaluateArguments(pool, args, current+1, soFarArgs << currentArgument, result, original, pooledThreadFlag)
        }
    }
}

def asyncFun(final Closure original) {
    final def pool = GParsPool.retrieveCurrentPool()
    return {final Object[] args ->
        final DataFlowVariable result = new DataFlowVariable()
        evaluateArguments(pool, args.clone(), 0, [], result, original, false)
        result
    }
}

withPool {
//    def result = ((asyncFun {a, b -> a + b}).call(1, new DataFlowVariable() << 2))
    def result = (0..100).inject(0, asyncFun{a, b -> a + b})
    println "Doing something else while the calculation is running"

    sleep 1000
    println "Are we done yet? ${result.bound}"
    if (!result.bound) println "Let's do something else then, since the calculation is still running"

    sleep 1000
    println "Now really, are we done yet? ${result.bound}"

    println "OK, I've run out of patience. I'll sit down here and wait for you to finish my calculation!"
    println result.val
}
