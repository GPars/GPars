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

package groovyx.gpars.samples.forkjoin

import groovyx.gpars.AbstractForkJoinWorker
import groovyx.gpars.ForkJoinOrchestrator
import groovyx.gpars.Parallelizer
import jsr166y.forkjoin.TaskBarrier

/**
 * Shows use of the ForkJoin mechanics to implement merge sort.
 *
 * Author: Vaclav Pech
 * Date: Jul 16, 2008
 */

public final class SortWorker extends AbstractForkJoinWorker<List<Integer>> {
    private final List numbers

    def SortWorker(final List<Integer> numbers) {
        this.numbers = numbers
    }

    def SortWorker(final List<Integer> numbers, final TaskBarrier taskBarrier) {
        super(taskBarrier)
        this.numbers = numbers
    }

    def split(List<Integer> list) {
        int listSize = list.size()
        int middleIndex = listSize / 2
        def list1 = list[0..<middleIndex]
        def list2 = list[middleIndex..listSize - 1]
        return [list1, list2]
    }

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

    protected void compute() {
        println "Sorting $numbers"
        switch (numbers.size()) {
            case 0..1:
                this.result = numbers
                break
            case 2:
                if (numbers[0] <= numbers[1]) this.result = numbers
                else this.result = numbers[-1..0]
                break
            default:
                def splitList = split(numbers)
                final TaskBarrier childTaskBarrier = new TaskBarrier(1)
                def workers = [new SortWorker(splitList[0], childTaskBarrier), new SortWorker(splitList[1], childTaskBarrier)]
                workers*.fork()
                childTaskBarrier.arriveAndAwait()
                this.result = merge(* workers*.result)
        }
    }
}

/**
 Fork/Join operations can be safely run with small number of threads thanks to using the TaskBarrier class to synchronize the threads.
 Although the algorithm creates tasks for all fractions of the original list and tasks wait for the sub-fractions' tasks to complete,
 as few as one thread is enough to keep the computation going.
 */

Parallelizer.doParallel(1) {pool ->  //feel free to experiment with the number of fork/join threads in the pool
    final def nums = [1, 5, 2, 4, 3, 8, 6, 7, 3, 4, 5, 2, 2, 9, 8, 7, 6, 7, 8, 1, 4, 1, 7, 5, 8, 2, 3, 9, 5, 7, 4, 3]
    final ForkJoinOrchestrator orchestrator = new ForkJoinOrchestrator(rootWorker: new SortWorker(nums))
    pool.execute(orchestrator)
//    pool.execute(new Sorter(numbers:nums, result:result))
    println "Sorted numbers: ${orchestrator.result}"
}
