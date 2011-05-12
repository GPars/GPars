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

package groovyx.gpars

import groovyx.gpars.dataflow.Dataflows

class ReactorMessagingRunnableTest extends GroovyTestCase {
    public void testMessagingRunnable() {
        final def df = new Dataflows()
        final def runnable = new MyTestReactorMessagingRunnable(df)
        shouldFail(UnsupportedOperationException) {
            runnable.call()
        }
        assert 'Result: 10' == runnable.call(10)
        assert 'Result: 20' == runnable.call([20] as Object[])
        assert df[10] == 10
        assert df[20] == 20
        shouldFail(UnsupportedOperationException) {
            runnable.call([30, 40] as Object[])
        }
    }

    public void testMaxNumberOfParameters() {
        final def runnable = new MyTestReactorMessagingRunnable(null)
        assert 1 == runnable.maximumNumberOfParameters
    }

    public void testParameterTypes() {
        final def runnable = new MyTestReactorMessagingRunnable(null)
        assert [Object] as Class[] == runnable.parameterTypes
    }
}

class MyTestReactorMessagingRunnable extends ReactorMessagingRunnable<Integer, String> {

    def df

    def MyTestReactorMessagingRunnable(df) {
        this.df = df
    }

    protected String doRun(final Integer argument) {
        df[argument] = argument
        return "Result: $argument"
    }
}
