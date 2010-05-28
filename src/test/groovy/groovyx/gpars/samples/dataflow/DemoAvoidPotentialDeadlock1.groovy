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

package groovyx.gpars.samples.dataflow

import groovyx.gpars.dataflow.DataFlowPGroup
import groovyx.gpars.dataflow.DataFlowVariable

/**
 * Demonstrates pool resizing. The code would end up deadlocked if the pool didn't resize, since the first two tasks
 * wait for each other to bind values to a and b. Only the third thread can unlock the two threads by setting value of a.
 *
 * @author Vaclav Pech
 */

final DataFlowPGroup group = new DataFlowPGroup(1)

final def a = new DataFlowVariable()
final def b = new DataFlowVariable()

group.with {
    task {
        b << 20 + a.val
    }

    task {
        println "Result: ${b.val}"
        System.exit 0
    }

    Thread.sleep 2000

    task {
        a << 10
    }

}