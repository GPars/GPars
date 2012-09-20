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

package groovyx.gpars.benchmark.dataflow

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.scheduler.FJPool
import groovyx.gpars.dataflow.operator.component.GracefulShutdownMonitor
import groovyx.gpars.dataflow.operator.component.GracefulShutdownListener
import groovyx.gpars.dataflow.operator.DataflowProcessor

final def concurrencyLevel = 8
3.times {
    perform("Direct terminate", concurrencyLevel, null, null)
}
3.times {
    final monitor = new GracefulShutdownMonitor(300)
    perform("Graceful shutdown", concurrencyLevel, monitor, new GracefulShutdownListener(monitor))
}

3.times {
    final monitor = new GracefulShutdownMonitor(100)
    perform("Early graceful shutdown", concurrencyLevel, monitor, new GracefulShutdownListener(monitor), true)
}

private void perform(String title, int concurrencyLevel, GracefulShutdownMonitor monitor, GracefulShutdownListener listener, boolean shutdownEarly=false) {
    group = new DefaultPGroup(new FJPool(concurrencyLevel))

    final DataflowQueue queue1 = new DataflowQueue()
    final DataflowQueue queue2 = new DataflowQueue()
    final DataflowQueue result = new DataflowQueue()

    (1..2000000).each {
        queue1 << it
        queue2 << 10
    }
    queue1 << -1
    queue2 << -1

    final def t1 = System.currentTimeMillis()

    long sum = 0
    def op = createOperator(queue1, queue2, result, sum, listener)

    if(listener==null) {
        assert 2000021000000 == result.val
        op.terminate()
    }
    else {
        if (!shutdownEarly) assert 2000021000000 == result.val
        monitor.shutdownNetwork().get()
    }
    op.join()
    group.shutdown()
    final def t2 = System.currentTimeMillis()
    println(title + ": " + (t2 - t1))
}

private DataflowProcessor createOperator(DataflowQueue queue1, DataflowQueue queue2, DataflowQueue result, long sum, GracefulShutdownListener listener) {
    group.operator(inputs: [queue1, queue2], outputs: [result], listeners: listener ? [listener] : []) {x, y ->
        if (x == -1) {
            bindOutput sum
        } else {
            sum += x + y
        }
    }
}



