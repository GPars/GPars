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

package groovyx.gpars.util

import groovyx.gpars.dataflow.DataFlowVariable
import groovyx.gpars.scheduler.Pool

/**
 *
 * @author Vaclav Pech
 * Date: 6th Sep 2010
 */
abstract class PAGroovyUtils {

    static java.util.Collection createCollection(Object object) {
        def collection = []
        for (element in object) collection << element
        return collection
    }

    /**
     * Performs a single step in the evaluation of parameters passed into an asynchronous function
     * @param pool The thread pool to use
     * @param args The list of original arguments
     * @param current The index of the current argument to evaluate
     * @param soFarArgs A list of arguments evaluated so far
     * @param result The DFV expecting the function result to be bound to once calculated
     * @param original The original non-asynchronous function to invoke once all arguments are available
     * @param pooledThreadFlag Indicates, whether we now run in a pooled thread so we don't have to schedule the original function invocation, once all arguments have been bound
     */
    static void evaluateArguments(final Pool pool, final args, final current, final soFarArgs, final result, final original, final pooledThreadFlag) {
        if (current == args.size()) {
            if (pooledThreadFlag) {
                try {
                    result << original(* soFarArgs)
                } catch (all) {
                    result << all
                }
            }
            else {
                pool.execute({->
                    try {
                        result << original(* soFarArgs)
                    } catch (all) {
                        result << all
                    }
                })
            }
        }
        else {
            def currentArgument = args[current]
            if (currentArgument instanceof DataFlowVariable) {
                currentArgument.whenBound(pool) {value ->
                    if (value instanceof Throwable) result << value
                    else evaluateArguments(pool, args, current + 1, soFarArgs << value, result, original, true)
                }
            } else {
                evaluateArguments(pool, args, current + 1, soFarArgs << currentArgument, result, original, pooledThreadFlag)
            }
        }
    }
}
