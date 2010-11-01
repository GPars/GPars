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

package groovyx.gpars.dataflow

import java.util.concurrent.Callable

public class DataFlowTaskTest extends GroovyTestCase {

    public void testTaskWithRunnable() {
        def a = new DataFlowVariable()
        DataFlow.task(new TestRunnable(a))

        assert a.val == 10
    }

    public void testTaskWithCallable() {
        def a = new DataFlowVariable()
        def result = DataFlow.task(new TestCallable(a))

        assert a.val == 10
        assert result.val == 20
    }

    public void testTaskWithClosure() {
        def a = new DataFlowVariable()
        def result = DataFlow.task {
            a << 10
            return 20
        }

        assert a.val == 10
        assert result.val == 20
    }
}

private class TestRunnable implements Runnable {

    def df

    def TestRunnable(def df) {
        this.df = df
    }

    public void run() {
        df << 10
    }
}

private class TestCallable implements Callable {

    def df

    def TestCallable(def df) {
        this.df = df
    }

    public Object call() {
        df << 10
        return 20
    }
}
