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

/**
 * Demonstrates asynchronous processing using the GParsExecutorsPool class.
 */

groovyx.gpars.GParsExecutorsPool.withPool {
    /**
     * The callAsync() method is an asynchronous variant of the default call() method to invoke a closure.
     * It will return a Future for the result value.
     */
    assert 6 == {it * 2}.callAsync(3).get()

    /**
     * An asynchronous variant of a closure is created using the async() method.
     * When invoked, it will returned a Future for the calculated value.
     */
    def asyncDoubler = {it * 2}.async()
    assert 20 == asyncDoubler(10).get()
    assert [2, 4, 6] == [1, 2, 3].collect(asyncDoubler)*.get()

    /**
     * Run multiple asynchronous closures in parallel by combining GParsExecutorsPool methods with lists and operators
     */
    final Closure doubler = {it * 2}
    final Closure modulo2 = {it % 2}
    final Closure exp2 = {2 ** it}

    def results = []
    [1, 2, 3, 4, 5].each {num ->
        results << [doubler, {it * 3}, {it ** 2}, exp2, modulo2]*.callAsync(num)
    }
    for (result in results) println result*.get()
}
