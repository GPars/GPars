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

/**
 * A classical example of map/reduce counting word occurrences in text, implemented in two ways.
 *
 */

package groovyx.gpars.samples

import groovyx.gpars.PAWrapper
import static groovyx.gpars.GParsPool.withPool

def words = """The xxxParallel() methods have to follow the contract of their non-parallel peers. So a collectParallel() method must return a legal collection of items, which you can again treat as a Groovy collection. Internally the parallel collect method builds an efficient parallel structure, called parallel array, performs the required operation concurrently and before returning destroys the Parallel Array building the collection of results to return to you. A potential call to let say findAllParallel() on the resulting collection would repeat the whole process of construction and destruction of a Parallel Array instance under the covers. With Map/Reduce you turn your collection into a Parallel Array and back only once. The Map/Reduce family of methods do not return Groovy collections, but are free to pass along the internal Parallel Arrays directly. Invoking the parallel property on a collection will build a Parallel Array for the collection and return a thin wrapper around the Parallel Array instance. Then you can chain all required methods like:""".tokenize()
println count(words)
println directCount(words)
println combineCount(words)
println directCombineCount(words)

def count(arg) {
    withPool {
        return arg.parallel.map {[it, 1]}.groupBy {it[0]}.getParallel().map {it.value = it.value.size(); it}.sort {-it.value}.collection
    }
}


def directCount(arg) {
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

def directCombineCount(arg) {
    withPool {
        return arg.parallel.map {[it, 1]}.combine({0}, {a, b -> a + b}).getParallel().sort {-it.value}.collection
//        return arg.parallel.map{[it, 1]}.combine({[]}, {list, value -> list << value}).getParallel().map{it.value = it.value.size();it}.sort{-it.value}.collection
//        arg.parallel.map{[it, 1]}.combine([]){list, value -> list+=value}.reduce{value->value.sum()}
    }
}

def combineCount(arg) {
    withPool {
        def myCombiner = new Combiner() {

            def Object createKey(Object element) {
                return element
            }

            def Object createValue(Object element) {
                return 1
            }

            def Object combine(Object a, Object b) {
                return a + b
            }

        }

        final PAWrapper parallel = arg.parallel
        return combine(parallel, myCombiner).getParallel().sort {-it.value}.collection
//        return arg.parallel.map{[it, 1]}.combine {a, b -> a + b}.getParallel().sort {-it.value}.collection
    }
}

private def combine(PAWrapper pa, myCombiner) {
    return pa.map {[(myCombiner.createKey(it)): myCombiner.createValue(it)]}.reduce {a, b ->
        b.each {k, v ->
            def value = a[k]
            if (value == null) a[k] = v
            else a[k] = myCombiner.combine(v, value)
        }
        a
    }
}

interface Combiner {
    Object createKey(element)

    Object createValue(element)

    Object combine(a, b)
}
