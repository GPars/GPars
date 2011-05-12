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

package groovyx.gpars.samples.dataflow

import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.group.PGroup
import groovyx.gpars.scheduler.DefaultPool

/**
 * Demonstrates deadlock prevention using the whenBound (>>) dataflow construct. Instead of blocking the task
 * together with its underlying thread on a read call to DataflowVariable.val, the whenBound handler will be invoked only
 * after the value of the DataflowVariable is set and so the reader doesn't consume a thread while waiting for a value
 * to arrive.
 * The code would end up deadlocked if we blocked the threads during reads, since the first two tasks
 * wait for each other to bind values to a and b. Only the third thread can unlock the two threads by setting value of a.
 *
 * @author Vaclav Pech
 */

final PGroup group = new DefaultPGroup(new DefaultPool(true, 1))

final def a = new DataflowVariable()
final def b = new DataflowVariable()

group.with {
    task {
        a >> {b << 20 + it}
    }

    task {
        b >> {
            println "Result: ${it}"
            group.shutdown()
        }
    }

    Thread.sleep 2000

    task {
        a << 10
    }
    b.join()
}