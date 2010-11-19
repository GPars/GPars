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

package groovyx.gpars.samples.dataflow.operators

import groovyx.gpars.dataflow.DataFlowQueue
import groovyx.gpars.dataflow.DataFlowVariable
import static groovyx.gpars.dataflow.DataFlow.operator
import static groovyx.gpars.dataflow.DataFlow.prioritySelector
import static groovyx.gpars.dataflow.DataFlow.task

/**
 * Shows a possible way to combine operators with PrioritySelectors.
 * Note that dataflow variables and streams can be combined for Selectors.
 * Unlike plain Selector, the PrioritySelector class gives precedence to input channels with lower index.
 * Available messages from high priority channels will be served before messages from lower-priority channels.
 * Messages received through a single input channel will have their mutual order preserved.
 *
 * Operators can take output from a Select or PrioritySelect as one of its (many) inputs, allowing channel prioritization
 * to be built into dataflow operator networks.
 */

def critical = new DataFlowVariable()
def ordinary = new DataFlowQueue()
def whoCares = new DataFlowQueue()

task {
    ordinary << 'All working fine'
    whoCares << 'I feel a bit tired'
    ordinary << 'We are on target'
}

task {
    ordinary << 'I have just started work. Will come back later...'
    sleep 5000
    ordinary << 'I am done for now'
}

task {
    whoCares << 'Huh, what is that noise'
    ordinary << 'Here I am to do some clean-up work'
    whoCares << 'I wonder whether unplugging this cable will eliminate that nasty sound.'
    critical << 'The server room goes on UPS!'
    whoCares << 'The sound has disappeared'
}

def selected = new DataFlowQueue()
def selector = prioritySelector(inputs: [critical, ordinary, whoCares], outputs: [selected])
def results = new DataFlowQueue()
def op = operator(selected, results, {bindOutput it})

println 'Starting to monitor our IT department'
sleep 3000
10.times {println "Received: ${results.val}"}

selector.stop()
op.stop()