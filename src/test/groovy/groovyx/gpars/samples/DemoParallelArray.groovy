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
 * Demonstrates several parallel algorithms using the low-level jsr-166y Parallel Array API.
 *
 * @author Vaclav Pech
 * Date: Oct 31, 2010
 */

package groovyx.gpars.samples

import jsr166y.forkjoin.Ops.Mapper
import jsr166y.forkjoin.Ops.Predicate
import jsr166y.forkjoin.Ops.Reducer

groovyx.gpars.GParsPool.withPool {
    assert 15 == [1, 2, 3, 4, 5].parallelArray.reduce({a, b -> a + b} as Reducer, 0)                                        //summarize
    assert 55 == [1, 2, 3, 4, 5].parallelArray.withMapping({it ** 2} as Mapper).reduce({a, b -> a + b} as Reducer, 0)       //summarize squares
    assert 20 == [1, 2, 3, 4, 5].parallelArray.withFilter({it % 2 == 0} as Predicate)                                       //summarize squares of even numbers
            .withMapping({it ** 2} as Mapper).reduce({a, b -> a + b} as Reducer, 0)

    assert 20 == (1..5).parallelArray                                                                                       //summarize squares of even numbers using sum
            .withFilter({it % 2 == 0} as Predicate).withMapping({it ** 2} as Mapper).reduce({a, b -> a + b} as Reducer, 0)

    def n = 10
    println((1..n).parallelArray.reduce({a, b -> a * b} as Reducer, 0))

    final def bitSizes = [4, 6, 8, 1, 4, 2, 4, 5, 7, 6, 7, 3, 2, 4, 5, 6, 7, 2, 1, 2]
    assert 256 == bitSizes.parallelArray.withMapping({2 ** it} as Mapper).max()                                             //find max value range


    assert 'abc' == 'abc'.parallelArray.reduce({a, b -> a + b} as Reducer, "")                                              //concatenate
    assert 'aa:bb:cc:dd:ee' == 'abcde'.parallelArray                                                                        //concatenate duplicated characters with separator
            .withMapping({it * 2} as Mapper).reduce({a, b -> "$a:$b"} as Reducer, "")
    //filter out some elements
    assert 'aa-bb-dd' == 'abcde'.parallelArray.withFilter({it != 'e'} as Predicate).withMapping({it * 2} as Mapper).all().withFilter({it != 'cc'} as Predicate).all().reduce({a, b -> "$a-$b"} as Reducer, null)
}


