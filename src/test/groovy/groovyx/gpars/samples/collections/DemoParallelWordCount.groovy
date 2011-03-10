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

import static groovyx.gpars.GParsPool.withPool

/**
 * A classical example of map/reduce counting word occurrences in text, implemented in several slightly different ways.
 */

def words = """The xxxParallel() methods have to follow the contract of their non-parallel peers. So a collectParallel() method must return a legal collection of items, which you can again treat as a Groovy collection. Internally the parallel collect method builds an efficient parallel structure, called parallel array, performs the required operation concurrently and before returning destroys the Parallel Array building the collection of results to return to you. A potential call to let say findAllParallel() on the resulting collection would repeat the whole process of construction and destruction of a Parallel Array instance under the covers. With Map/Reduce you turn your collection into a Parallel Array and back only once. The Map/Reduce family of methods do not return Groovy collections, but are free to pass along the internal Parallel Arrays directly. Invoking the parallel property on a collection will build a Parallel Array for the collection and return a thin wrapper around the Parallel Array instance. Then you can chain all required methods like:""".tokenize()
println groupByCount(words)
println mapReduceCount(words)
println combineCount1(words)
println combineCount2(words)

/**
 * Uses the groupBy operation to join elements with the same key into a list and then, as a separate step, joins all the values associated around the same key
 */
def groupByCount(arg) {
    withPool {
        return arg.parallel.map {[it, 1]}.groupBy {it[0]}.getParallel().map {it.value = it.value.size(); it}.sort {-it.value}.collection
    }
}

/**
 * Directly turns each tuple into a hash map and and then reduces by merging the maps
 */
def mapReduceCount(arg) {
    withPool {
        return arg.parallel.map {[(it): 1]}.reduce {a, b ->
            b.each {k, v ->
                def value = a[k]
                if (value == null) a[k] = v
                else a[k] = value + v
            }
            a
        }.getParallel().sort {-it.value}.collection
    }
}

/**
 * Uses the combine operation to join elements with the same key and also, at the same time, joins all the values associated around the same key using the provided accumulation function
 */
def combineCount1(arg) {
    withPool {
        arg.parallel.map {[it, 1]}.combine(0, {a, b -> a + b}).getParallel().sort {-it.value}.collection
    }
}

/**
 * Uses the combine operation to join elements with the same key into a list and then, as a separate step, joins all the values associated around the same key using the provided accumulation operation
 */
def combineCount2(arg) {
    withPool {
        arg.parallel.map {[it, 1]}.combine([], {list, value -> list << value}).getParallel().map {it.value = it.value.size(); it}.sort {-it.value}.collection
//        arg.parallel.map {[it, 1]}.combine([], {list, value -> list << value}).getParallel().map {k, v -> v = v.value.size(); it}.sort {-it.value}.collection
    }
}

