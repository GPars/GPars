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

package groovyx.gpars

import groovy.time.Duration
import groovyx.gpars.dataflow.DataFlowVariable
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * @author Vaclav Pech
 * Date: Aug 18, 2010
 */
class AsyncInvocationWithTimeoutTest extends GroovyTestCase {

    public void testFastCalculation() {
        GParsExecutorsPool.withPool(5) {
            assert 10 == {-> 10}.callTimeoutAsync(5000L).get()
        }
        GParsPool.withPool(5) {
            assert 10 == {-> 10}.callTimeoutAsync(5000L).get()
            Closure cl = {}
        }
    }

    public void testSlowCalculation() {
        GParsExecutorsPoolCalculation(10)
        GParsExecutorsPoolCalculation(new Duration(0, 0, 0, 0, 10))
        GParsPoolCalculation(10)
        GParsPoolCalculation(new Duration(0, 0, 0, 0, 10))
    }

    private def GParsExecutorsPoolCalculation(timeout) {
        return GParsExecutorsPool.withPool(5) {
            calculate(timeout)
        }
    }

    private def GParsPoolCalculation(timeout) {
        return GParsExecutorsPool.withPool(5) {
            calculate(timeout)
        }
    }

    private def calculate(timeout) {
        shouldFail(CancellationException) {
            {-> Thread.sleep 5000; 20}.callTimeoutAsync(timeout).get()
        }

        shouldFail(TimeoutException) {
            {-> Thread.sleep 5000; 20}.callTimeoutAsync(1000).get(50, TimeUnit.MILLISECONDS)
        }

        final DataFlowVariable result = new DataFlowVariable();
        {-> try {Thread.sleep 2000; result << 20} catch (all) {}}.callTimeoutAsync(timeout);
        sleep 3000
        assert !result.bound
    }

}
