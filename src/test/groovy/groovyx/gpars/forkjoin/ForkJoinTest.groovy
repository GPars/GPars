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

import java.util.concurrent.ExecutionException
import static groovyx.gpars.GParsPool.runForkJoin
import static groovyx.gpars.GParsPool.withPool

/**
 *
 * @author Vaclav Pech
 * Date: Jan 15, 2010
 */

class ForkJoinTest extends GroovyTestCase {
    public void testMergeSort() {
        final def numbers = [1, 5, 2, 4, 3, 8, 6, 7, 3, 4, 5]

        withPool(3) {
            assertArrayEquals([1, 2, 3, 3, 4, 4, 5, 5, 6, 7, 8].toArray(), runForkJoin(new TestSortWorker(numbers)).toArray())
        }
        withPool(1) {
            final TestSortWorker worker = new TestSortWorker(numbers)
            assertArrayEquals([1, 2, 3, 3, 4, 4, 5, 5, 6, 7, 8].toArray(), runForkJoin(worker).toArray())
            assert [] == worker.childrenResults
        }
    }

    public void testMergeSortWithException() {
        final def numbers = [1, 5, 2, 4, 'abc', 8, 6, 7, 3, 4, 5]

        final TestSortWorker worker = new TestSortWorker(numbers)
        withPool(3) {
            shouldFail(ExecutionException) {
                runForkJoin(worker)
            }
        }
        assert [] == worker.childrenResults
    }

    public void testChildrenCollection() {
        TestSortWorker worker = new TestSortWorker([] as List<Integer>)
        assert [] == worker.childrenResults
        assert [] == worker.compute()
        assert [] == worker.childrenResults

        worker = new TestSortWorker([1, 2, 3, 4, 5] as List<Integer>)
        assert [] == worker.childrenResults
    }
}

public final class TestSortWorker extends AbstractForkJoinWorker<List<Integer>> {
    private final List<Integer> numbers

    def TestSortWorker(final List<Integer> numbers) {
        this.numbers = numbers.asImmutable()
    }

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

    /**
     * Sorts a small list or delegates to two children, if the list contains more than two elements.
     */
    @Override
    List<Integer> computeTask() {
        switch (numbers.size()) {
            case 0..1:
                return numbers                                   //store own result
            case 2:
                if (numbers[0] <= numbers[1]) return numbers     //store own result
                else return numbers[-1..0]                       //store own result
            default:
                def splitList = split(numbers)
                [new TestSortWorker(splitList[0]), new TestSortWorker(splitList[1])].each {forkOffChild it}  //fork a child task
                getChildrenResults()  //to test re-entrance capability of the children results collection
                if (getChildrenResults().size() != 2) throw new IllegalStateException("Number of children results ${getChildrenResults().size()} is invalid.")
                return merge(* childrenResults)      //use results of children tasks to calculate and store own result
        }
    }
}
