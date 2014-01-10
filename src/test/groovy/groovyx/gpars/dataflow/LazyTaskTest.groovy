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

import groovyx.gpars.group.DefaultPGroup

import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

public class LazyTaskTest extends GroovyTestCase {
    def group

    @Override
    protected void setUp() throws Exception {
        super.setUp()
        group = new DefaultPGroup()
    }

    @Override
    protected void tearDown() throws Exception {
        group.shutdown()
        group = null
        super.tearDown()
    }

    public void testVariable() {
        final LazyDataflowVariable variable = group.lazyTask { -> 10 }
        assert 10 == variable.get()
        assert 10 == variable.get()

        shouldFail(IllegalStateException) {
            variable << 20
        }

        shouldFail(IllegalStateException) {
            final def v = new LazyDataflowVariable({ -> 20 })
            v.get()
            variable << v
        }
        assert 10 == variable.val
    }

    public void testGet() {
        final LazyDataflowVariable variable = group.lazyTask { -> 10 }
        assert 10 == variable.get()
        assert 10 == variable.get()
        assert 10 == variable.get(10, java.util.concurrent.TimeUnit.SECONDS)
    }

    public void testGetOnCallable() {
        final LazyDataflowVariable variable = group.lazyTask(new Callable<Integer>() {
            @Override
            Integer call() throws Exception {
                return 10
            }
        })
        assert 10 == variable.get()
        assert 10 == variable.get()
        assert 10 == variable.get(10, java.util.concurrent.TimeUnit.SECONDS)
    }

    public void testTimeoutGet() {
        final LazyDataflowVariable variable = group.lazyTask { -> 10 }
        assert 10 == variable.get(10, java.util.concurrent.TimeUnit.SECONDS)
        assert 10 == variable.get()
    }

    public void testGetException() {
        final LazyDataflowVariable variable = group.lazyTask { -> throw new Exception('test') }
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
        final LazyDataflowVariable variable = group.lazyTask { -> throw new RuntimeException('test') }
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
}
