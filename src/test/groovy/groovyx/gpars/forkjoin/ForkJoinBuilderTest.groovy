// GPars (formerly GParallelizer)
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

package groovyx.gpars.forkjoin

import java.util.concurrent.ExecutionException
import static groovyx.gpars.ForkJoinPool.orchestrate
import static groovyx.gpars.ForkJoinPool.withPool

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
                break
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
            assertArrayEquals([1, 2, 3, 3, 4, 4, 5, 5, 6, 7, 8].toArray(), orchestrate(numbers, mergeSortCode).toArray())
        }
        withPool(1) {
            final TestSortWorker worker = new TestSortWorker(numbers)
            assertArrayEquals([1, 2, 3, 3, 4, 4, 5, 5, 6, 7, 8].toArray(), orchestrate(numbers, mergeSortCode).toArray())
            assert [] == worker.getChildrenResults()
        }
    }

    public void testMergeSortWithException() {
        final def numbers = [1, 5, 2, 4, 'abc', 8, 6, 7, 3, 4, 5]

        withPool(3) {
            shouldFail(ExecutionException) {
                orchestrate(numbers, mergeSortCode)
            }
        }
    }

    public void testMultipleArguments() {
        final def numbers = [1, 5, 2, 4, 3, 8, 6, 7, 3, 4, 5]

        withPool(3) {
            assertArrayEquals([1].toArray(), groovyx.gpars.ForkJoinPool.orchestrate(numbers, 'foo', true) {nums, stringValue, booleanValue ->
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
        final def numbers = [1, 5, 2, 4, 3, 8, 6, 7, 3, 4, 5]

        withPool(3) {
            shouldFail(IllegalArgumentException) {
                groovyx.gpars.ForkJoinPool.orchestrate()
            }
            shouldFail(IllegalArgumentException) {
                groovyx.gpars.ForkJoinPool.orchestrate(1, 2, 3)
            }
            shouldFail(IllegalArgumentException) {
                groovyx.gpars.ForkJoinPool.orchestrate(1, 2, 3) {}
            }
            shouldFail(IllegalArgumentException) {
                groovyx.gpars.ForkJoinPool.orchestrate(1, 2, 3) {->}
            }
            shouldFail(IllegalArgumentException) {
                groovyx.gpars.ForkJoinPool.orchestrate(1) {->}
            }
            shouldFail(IllegalArgumentException) {
                groovyx.gpars.ForkJoinPool.orchestrate(1, 2, 3) {a ->}
            }
            shouldFail(IllegalArgumentException) {
                groovyx.gpars.ForkJoinPool.orchestrate(1, 2, 3) {a, b ->}
            }
            shouldFail(IllegalArgumentException) {
                groovyx.gpars.ForkJoinPool.orchestrate() {a, b ->}
            }
            shouldFail(IllegalArgumentException) {
                groovyx.gpars.ForkJoinPool.orchestrate(1) {a, b ->}
            }
        }
    }
}
