// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
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

package groovyx.gpars.forkjoin

import groovyx.gpars.dataflow.Dataflows
import java.util.concurrent.ExecutionException
import static groovyx.gpars.GParsPool.runForkJoin
import static groovyx.gpars.GParsPool.withPool

/**
 *
 * @author Vaclav Pech
 * Date: Jan 15, 2010
 */

class ForkJoinBuilderTest extends GroovyTestCase {
    /**
     * Splits a list of numbers in half
     */
    def split(List<Integer> list) {
        int listSize = list.size()
        int middleIndex = listSize / 2
        def list1 = list[0..<middleIndex]
        def list2 = list[middleIndex..listSize - 1]
        return [list1, list2]
    }

    /**
     * Merges two sorted lists into one
     */
    List<Integer> merge(List<Integer> a, List<Integer> b) {
        int i = 0, j = 0
        final int newSize = a.size() + b.size()
        List<Integer> result = new ArrayList<Integer>(newSize)

        while ((i < a.size()) && (j < b.size())) {
            if (a[i] <= b[j]) result << a[i++]
            else result << b[j++]
        }

        if (i < a.size()) result.addAll(a[i..-1])
        else result.addAll(b[j..-1])
        return result
    }

    final Closure mergeSortCode = {nums ->
        switch (nums.size()) {
            case 0..1:
                return nums                                   //store own result
            case 2:
                if (nums[0] <= nums[1]) return nums     //store own result
                else return nums[-1..0]                       //store own result
            default:
                def splitList = split(nums)
                [splitList[0], splitList[1]].each {forkOffChild it}  //fork a child task
                getChildrenResults()  //to test re-entrance capability of the children results collection
                if (getChildrenResults().size() != 2) throw new IllegalStateException("Number of children results ${getChildrenResults().size()} is invalid.")
                return merge(* childrenResults)      //use results of children tasks to calculate and store own result
        }
    }

    public void testMergeSort() {
        final def numbers = [1, 5, 2, 4, 3, 8, 6, 7, 3, 4, 5]

        withPool(3) {
            assertArrayEquals([1, 2, 3, 3, 4, 4, 5, 5, 6, 7, 8].toArray(), runForkJoin(numbers, mergeSortCode).toArray())
        }
        withPool(2) {
            final TestSortWorker worker = new TestSortWorker(numbers)
            assertArrayEquals([1, 2, 3, 3, 4, 4, 5, 5, 6, 7, 8].toArray(), runForkJoin(numbers, mergeSortCode).toArray())
            assert [] == worker.childrenResults
        }
    }

    public void testMergeSortWithException() {
        final def numbers = [1, 5, 2, 4, 'abc', 8, 6, 7, 3, 4, 5]

        withPool(3) {
            shouldFail(ExecutionException) {
                runForkJoin(numbers, mergeSortCode)
            }
        }
    }

    public void testMultipleArguments() {
        final def numbers = [1, 5, 2, 4, 3, 8, 6, 7, 3, 4, 5]

        withPool(3) {
            assertArrayEquals([1].toArray(), groovyx.gpars.GParsPool.runForkJoin(numbers, 'foo', true) {nums, stringValue, booleanValue ->
                if (nums.size() > 1) {
                    forkOffChild(nums.subList(0, nums.size() - 1), stringValue, booleanValue)
                    return getChildrenResults()[0]
                } else {
                    return nums
                }
            }.toArray())
        }
    }

    public void testIllegalArguments() {
        withPool(3) {
            shouldFail(IllegalArgumentException) {
                groovyx.gpars.GParsPool.runForkJoin()
            }
            shouldFail(IllegalArgumentException) {
                groovyx.gpars.GParsPool.runForkJoin(1, 2, 3)
            }
            shouldFail(IllegalArgumentException) {
                groovyx.gpars.GParsPool.runForkJoin(1, 2, 3) {}
            }
            shouldFail(IllegalArgumentException) {
                groovyx.gpars.GParsPool.runForkJoin(1, 2, 3) {->}
            }
            shouldFail(IllegalArgumentException) {
                groovyx.gpars.GParsPool.runForkJoin(1) {->}
            }
            shouldFail(IllegalArgumentException) {
                groovyx.gpars.GParsPool.runForkJoin(1, 2, 3) {a ->}
            }
            shouldFail(IllegalArgumentException) {
                groovyx.gpars.GParsPool.runForkJoin(1, 2, 3) {a, b ->}
            }
            shouldFail(IllegalArgumentException) {
                groovyx.gpars.GParsPool.runForkJoin() {a, b ->}
            }
            shouldFail(IllegalArgumentException) {
                groovyx.gpars.GParsPool.runForkJoin(1) {a, b ->}
            }
        }
    }

    public void testForkOffChildIllegalArguments() {
        final Dataflows df = new Dataflows()

        withPool(3) {
            groovyx.gpars.GParsPool.runForkJoin(1, 2) {a, b ->
                try {
                    forkOffChild(5)
                } catch (Throwable t) {
                    df.e1 = t
                }
            }
            groovyx.gpars.GParsPool.runForkJoin(1, 2) {a, b ->
                try {
                    forkOffChild(5, 2, 4)
                } catch (Throwable t) {
                    df.e2 = t
                }
            }
            groovyx.gpars.GParsPool.runForkJoin() {->
                try {
                    forkOffChild(5)
                } catch (Throwable t) {
                    df.e3 = t
                }
            }
            groovyx.gpars.GParsPool.runForkJoin(1, 2) {a, b ->
                try {
                    forkOffChild()
                } catch (Throwable t) {
                    df.e4 = t
                }
            }
            assert df.e1 instanceof IllegalArgumentException
            assert df.e2 instanceof IllegalArgumentException
            assert df.e3 instanceof IllegalArgumentException
            assert df.e4 instanceof IllegalArgumentException
        }
    }
}
