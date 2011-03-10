// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
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

package groovyx.gpars.samples.collections

import groovyx.gpars.GParsExecutorsPool

/**
 * Demonstrates asynchronous processing using the GParsExecutorsPool class.
 */

final Closure doubler = {it * 2}

GParsExecutorsPool.withPool {
/**
 * Using the dedicated methods to run multiple functions in parallel
 */
    println groovyx.gpars.GParsExecutorsPool.executeAsyncAndWait({it * 2}.curry(10), {doubler(10)}, doubler.curry(10), {doubler.call(10)}, {{num -> num * 2}.call(10)})

/**
 * The same thing, but without waiting for the results. Use the Future.get() method, once you need the result.
 */
    println groovyx.gpars.GParsExecutorsPool.executeAsync({it * 2}.curry(10), {doubler(10)}, doubler.curry(10), {doubler.call(10)}, {{num -> num * 2}.call(10)})*.get()

    final List names = [].asSynchronized()
    GParsExecutorsPool.executeAsyncAndWait({storeName(names, 'Joe')}, {storeName(names, 'Dave')}, {storeName(names, 'Alice')}, {storeName(names, 'Jason')}, {storeName(names, 'George')}, {storeName(names, 'Susan')})
    println 'Done name storage'
}

private def storeName(List names, name) {
    println "Storing $name"
    Thread.sleep 2000
    names << name
    println "Finished $name"
}
