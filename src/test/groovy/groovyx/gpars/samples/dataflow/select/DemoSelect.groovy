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

package groovyx.gpars.samples.dataflow.select

import groovyx.gpars.dataflow.DataFlowQueue
import groovyx.gpars.dataflow.DataFlowVariable
import static groovyx.gpars.dataflow.DataFlow.select
import static groovyx.gpars.dataflow.DataFlow.task

/**
 * Shows a basic use of Select, which monitors a set of input channels for values and makes these values
 * available on its output irrespective of their original input channel.
 * Note that dataflow variables and streams can be combined for Select.
 *
 * You might also consider checking out the prioritySelect method, which prioritizes values by the index of their input channel
 */
def a = new DataFlowVariable()
def b = new DataFlowVariable()
def c = new DataFlowQueue()

task {
    sleep 3000
    a << 10
}

task {
    sleep 1000
    b << 20
}

task {
    sleep 5000
    c << 30
}

def select = select(a, b, c)
println "The fastest result is ${select()}"
