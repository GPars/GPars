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

import static groovyx.gpars.GParsPool.runForkJoin
import static groovyx.gpars.GParsPool.withPool

class DirectChildrenCallTest extends GroovyTestCase {
    public void testDirectFib() {
        Closure fib = {number ->
            if (number <= 2) {
                return 1
            }
            forkOffChild(number - 1)
            final def result = runChildDirectly(number - 2)
            return (Integer) getChildrenResults().sum() + result
        }

        withPool {
            assert 55 == runForkJoin(10, fib)
        }
    }

    public void testCorrectThreading() {
        final def threads = [:].asSynchronized()
        Closure fib = {number ->
            if (number <= 2) {
                return 1
            }
            threads.put(Thread.currentThread(), 1)
            final def result1 = runChildDirectly(number - 1)
            final def result2 = runChildDirectly(number - 2)
            return result1 + result2
        }

        withPool {
            assert 55 == runForkJoin(10, fib)
            assert threads.size() == 1
        }
    }
}
