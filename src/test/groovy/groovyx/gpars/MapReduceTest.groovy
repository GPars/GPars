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

package groovyx.gpars

import java.util.concurrent.ConcurrentHashMap

/**
 * @author Vaclav Pech
 * Date: Nov 6, 2009
 */

public class MapReduceTest extends GroovyTestCase {

    public void testReduce() {
        Parallelizer.doParallel(5) {
            assertEquals 15, [1, 2, 3, 4, 5].parallel.map{it}.reduce {a, b -> a + b}
            assertEquals 'abc', 'abc'.parallel.map{it}.reduce {a, b -> a + b}
            assertEquals 55, [1, 2, 3, 4, 5].parallel.map{it ** 2}.reduce {a, b -> a + b}
            assertEquals 'aa:bb:cc:dd:ee', 'abcde'.parallel.map{it*2}.reduce{a, b -> "$a:$b"}
            assertEquals 'aa-bb-dd', 'abcde'.parallel.filter{it!='e'}.map{it*2}.filter{it !='cc'}.reduce{a, b -> "$a-$b"}
        }
    }

    public void testNestedMap() {
        Parallelizer.doParallel(5) {
            assertEquals 65, [1, 2, 3, 4, 5].parallel.map{it}.map{it+10}.reduce {a, b -> a + b}
        }
    }

    public void testMapFilter() {
        Parallelizer.doParallel(5) {
            assert([4, 5].containsAll([1, 2, 3, 4, 5].parallel.map{it}.filter{it > 3}.collection))
            assertEquals 9, [1, 2, 3, 4, 5].parallel.map{it}.filter{ it > 3 }.map {it}.reduce {a, b -> a + b }
        }
    }

    public void testFilterMap() {
        Parallelizer.doParallel(5) {
            assertEquals 9, [1, 2, 3, 4, 5].parallel.filter{it>3}.map{it}.reduce {a, b -> a + b}
        }
    }

    public void testReduceThreads() {
        final ConcurrentHashMap map = new ConcurrentHashMap()

        Parallelizer.doParallel(5) {
            assertEquals 55, [1, 2, 3, 4, 5, 6, 7, 8, 9, 10].parallel.map{it}.reduce {a, b ->
                Thread.sleep 200
                map[Thread.currentThread()] = ''
                a + b
            }
            assert map.keys().size() > 1
        }
    }

    public void testMinMax() {
        Parallelizer.doParallel(5) {
            assertEquals 1, [1, 2, 3, 4, 5].parallel.map{it}.min {a, b -> a - b}
            assertEquals 1, [1, 2, 3, 4, 5].parallel.map{it}.min {it}
            assertEquals 1, [1, 2, 3, 4, 5].parallel.map{it}.min()
            assertEquals 5, [1, 2, 3, 4, 5].parallel.map{it}.max {a, b -> a - b}
            assertEquals 5, [1, 2, 3, 4, 5].parallel.map{it}.max {it}
            assertEquals 5, [1, 2, 3, 4, 5].parallel.map{it}.max()
            assertEquals 'a', 'abc'.parallel.map{it}.min()
            assertEquals 'c', 'abc'.parallel.map{it}.max()
        }
    }

    public void testSum() {
        Parallelizer.doParallel(5) {
            assertEquals 15, [1, 2, 3, 4, 5].parallel.sum()
            assertEquals 'aabbccddee', 'abcde'.parallel.map{it*2}.sum()
        }
    }

    public void testCollectionProperty() {
        Parallelizer.doParallel(5) {
            final def original = [1, 2, 3, 4, 5]
            final def collection = original.parallel.collection
            assertEquals original, collection
            assert !original.is(collection)
            assert collection instanceof ArrayList
            assertEquals collection, collection.clone()
        }
    }

}