//  GPars (formerly GParallelizer)
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License. 

package groovyx.gpars.samples

import groovyx.gpars.Asynchronizer

/**
 * Demonstrates asynchronous processing using the Asynchronizer class.
 */

final Closure doubler = {it * 2}

/**
 * Using the dedicated methods to run multiple functions in parallel
 */
println Asynchronizer.doInParallel({it * 2}.curry(10), {doubler(10)}, doubler.curry(10), {doubler.call(10)}, {{num -> num * 2}.call(10)})

/**
 * The same thing, but without waiting for the results. Use the Future.get() method, once you need the result.
 */
println Asynchronizer.executeAsync({it * 2}.curry(10), {doubler(10)}, doubler.curry(10), {doubler.call(10)}, {{num -> num * 2}.call(10)})*.get()

/**
 * If the return values are not needed, the startInParallel() method will start a set of concurrently run closures
 * in a parallel thread, so the delay for the caller thread is reduced to the bare minimum.
 */
final List names = [].asSynchronized()
Asynchronizer.startInParallel({storeName(names, 'Joe')}, {storeName(names, 'Dave')}, {storeName(names, 'Alice')}, {storeName(names, 'Jason')}, {storeName(names, 'George')}, {storeName(names, 'Susan')})
println 'Started name storage'

private def storeName(List names, name) {
    println "Storing $name"
    Thread.sleep 2000
    names << name
    println "Finished $name"
}
