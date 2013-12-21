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

package groovyx.gpars.samples.forkjoin

import groovyx.gpars.forkjoin.AbstractForkJoinWorker

/**
 *
 * @author Vaclav Pech
 * Date: Feb 19, 2010
 */
import java.util.concurrent.ExecutionException

import static groovyx.gpars.GParsPool.withPool
import static groovyx.gpars.forkjoin.ForkJoinUtils.runForkJoin

class ForkJoinFib extends AbstractForkJoinWorker {

    def number

    @Override
    protected Integer computeTask() {
        if (number < 0) {
            throw new RuntimeException("No fib below 0!")
        }
        if (number <= 13) {
            return sequentialFib(number)
        }
        forkOffChild new ForkJoinFib(number: (number - 1))
        forkOffChild new ForkJoinFib(number: (number - 2))
        return (Integer) childrenResults.sum()
    }

    static int sequentialFib(int n) {
        if (n <= 1) return n;
        else return sequentialFib(n - 1) + sequentialFib(n - 2);
    }
}

withPool(2) { pool ->

    final long t1 = System.currentTimeMillis()
    try {
        assert runForkJoin(pool, new ForkJoinFib(number: 30)) == 832040

        assert ForkJoinFib.sequentialFib(31) == runForkJoin(pool, new ForkJoinFib(number: 31))

        try {
            runForkJoin(pool, new ForkJoinFib(number: -1))
        } catch (ExecutionException ignore) {
            println "We've correctly received an exception. That's what we deserve for calculating a negative Fibonacci number."
        }
    } catch (Throwable e) {
        e.printStackTrace()
    }
    final long t2 = System.currentTimeMillis()
    println t2 - t1
}

