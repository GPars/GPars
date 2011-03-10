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

import groovyx.gpars.dataflow.DataFlows

class DataFlowMessagingRunnableTest extends GroovyTestCase {
    public void testMessagingRunnable() {
        final def df = new DataFlows()
        final def runnable = new MyTestDataFlowMessagingRunnable(df)
        shouldFail(UnsupportedOperationException) {
            runnable.call()
        }
        runnable.call(5)
        assert df[5] == 5
        runnable.call([6] as Object[])
        assert df[6] == 6
        runnable.call([100, 200] as Object[])
        assert df[100] == 300

        runnable.call([200, 20, 30, 40, 50] as Object[])
        assert df[200] == 250

        runnable.call([1, 2, 3] as Object[])
        assert df[1] == 6
        runnable.call([10, 20, 30] as Object[])
        assert df[10] == 60
    }

    public void testMaxNumberOfParameters() {
        final def runnable = new MyTestDataFlowMessagingRunnable(null)
        assert 3 == runnable.maximumNumberOfParameters
    }

    public void testParameterTypes() {
        final def runnable = new MyTestDataFlowMessagingRunnable(null)
        assert [Object, Object, Object] as Class[] == runnable.parameterTypes
    }
}

class MyTestDataFlowMessagingRunnable extends DataFlowMessagingRunnable {

    def df

    def MyTestDataFlowMessagingRunnable(df) {
        super(3)
        this.df = df
    }

    protected void doRun(Object[] arguments) {
        df[arguments[0]] = arguments[0] + (arguments.size() > 1 ? arguments[1] : 0) + (arguments.size() > 2 ? arguments[2] : 0)
    }
}
