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
import static groovyx.gpars.agent.Agent.agent

/**
 * Demonstrates using Agent to count active tasks so that the main thread can ensure it exits only after all scheduled tasks finish their work.
 *
 * @author Vaclav Pech
 */


activeTasks = agent(0L)

pooledGroup = new DefaultPGroup(20)

println '*** Started at ' + new Date()
for (def i in 1..1000)
    process i

def doneFlag = new DataflowVariable()
activeTasks.addListener {oldValue, newValue -> if (newValue == 0) doneFlag.bind(true)}
if (activeTasks.val > 0) doneFlag.join()
pooledGroup.shutdown()
println '*** Ended at ' + new Date()


public void process(int i) {
    activeTasks << {updateValue it + 1}
    pooledGroup.task {
        Thread.sleep(100) // to simulate some work
        println 'Task ' + i + ' finished at ' + new Date()
        activeTasks << {updateValue it - 1}
    }
}

