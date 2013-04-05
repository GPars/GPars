// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2012  The original author or authors
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

package groovyx.gpars.dataflow

import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit

public class LazyDataflowVariableTest extends GroovyTestCase {

    public void testVariable() {
        final LazyDataflowVariable variable = new LazyDataflowVariable({-> 10 })
        assert 10 == variable.get()
        assert 10 == variable.get()

        shouldFail(IllegalStateException) {
            variable << 20
        }

        shouldFail(IllegalStateException) {
            final def v = new LazyDataflowVariable({-> 20 })
            v.get()
            variable << v
        }
        assert 10 == variable.val
    }

    public void testGet() {
        final LazyDataflowVariable variable = new LazyDataflowVariable({-> 10 })
        assert 10 == variable.get()
        assert 10 == variable.get()
        assert 10 == variable.get(10, java.util.concurrent.TimeUnit.SECONDS)
    }

    public void testTimeoutGet() {
        final LazyDataflowVariable variable = new LazyDataflowVariable({-> 10 })
        assert 10 == variable.get(10, java.util.concurrent.TimeUnit.SECONDS)
        assert 10 == variable.get()
    }

    public void testGetException() {
        final LazyDataflowVariable variable = new LazyDataflowVariable({-> throw new Exception('test') })
        shouldFail(Exception) {
            variable.get()
        }
        shouldFail(Exception) {
            variable.get()
        }
        shouldFail(Exception) {
            variable.get(10, TimeUnit.SECONDS)
        }
    }

    public void testGetRuntimeException() {
        final LazyDataflowVariable variable = new LazyDataflowVariable({-> throw new RuntimeException('test') })
        shouldFail(RuntimeException) {
            variable.get()
        }
        shouldFail(RuntimeException) {
            variable.get()
        }
        shouldFail(RuntimeException) {
            variable.get(10, TimeUnit.SECONDS)
        }
    }

    public void testToString() {
        final LazyDataflowVariable<Integer> variable = new LazyDataflowVariable<Integer>({-> 10 })
        assert 'LazyDataflowVariable(value=null)' == variable.toString()
        variable.get()
        assert 'LazyDataflowVariable(value=10)' == variable.toString()
        assert 'LazyDataflowVariable(value=10)' == variable.toString()
    }

    public void testVariablePoll() {
        final LazyDataflowVariable<Integer> variable = new LazyDataflowVariable<Integer>({-> 10 })
        final def result = new DataflowVariable()

        assert variable.poll() == null
        assert variable.poll() == null
        variable >> {
            result << variable.poll()
        }

        assert 10 == variable.val
        assert 10 == result.val
        assert 10 == result.poll().val
    }

    public void testJoin() {
        final LazyDataflowVariable variable = new LazyDataflowVariable({-> 10 })

        def t1 = System.currentTimeMillis()

        sleep 1000

        variable.join()
        assert 10 == variable.val

        variable.join()
        assert 10 == variable.val

        assert System.currentTimeMillis() - t1 < 60000
    }

    public void testTimedJoin() {
        final CyclicBarrier barrier = new CyclicBarrier(2)
        final LazyDataflowVariable variable = new LazyDataflowVariable({->
            barrier.await()
            10
        })

        def t1 = System.currentTimeMillis()

        variable.join(10, TimeUnit.MILLISECONDS)
        assert !variable.isBound()
        barrier.await()

        variable.join(10, TimeUnit.MINUTES)
        assert 10 == variable.val

        variable.join(10, TimeUnit.MINUTES)
        assert 10 == variable.val

        assert System.currentTimeMillis() - t1 < 60000
    }

    public void testEqualValueRebind() {
        final LazyDataflowVariable variable = new LazyDataflowVariable({-> [1, 2, 3] })
        variable.bind([1, 2, 3])
        variable.bind([1, 2, 3])
        variable.bindSafely([1, 2, 3])
        variable << [1, 2, 3]
        shouldFail(IllegalStateException) {
            variable.bind([1, 2, 3, 4, 5])
        }
        shouldFail(IllegalStateException) {
            variable << [1, 2, 3, 4, 5]
        }
        shouldFail(IllegalStateException) {
            variable.bindUnique([1, 2, 3])
        }
        shouldFail(IllegalStateException) {
            variable.bindUnique([1, 2, 3, 4, 5])
        }
    }

    public void testNullValueRebind() {
        final LazyDataflowVariable variable = new LazyDataflowVariable({ null })
        variable.bind(null)
        variable.bind(null)
        variable.bindSafely(null)
        variable << null
        shouldFail(IllegalStateException) {
            variable.bind(10)
        }
        shouldFail(IllegalStateException) {
            variable << 20
        }
        shouldFail(IllegalStateException) {
            variable.bindUnique(null)
        }
        shouldFail(IllegalStateException) {
            variable.bindUnique(30)
        }
    }

    public void testResultComposition() {

        final LazyDataflowVariable variable = new LazyDataflowVariable({->
            new LazyDataflowVariable({-> 10 })
        })
        assert variable.get() == 10

    }
}
