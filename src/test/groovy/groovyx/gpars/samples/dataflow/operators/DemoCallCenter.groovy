// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.samples.dataflow.operators

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.group.PGroup

/**
 * Motivation http://www.mprescient.com/journal/2011/1/9/concurrency-in-go-a-call-center-tutorial.html and https://gist.github.com/773979
 *
 * Using dataflow operators, which read messages from the queue asynchronously by nature. Can adopt to the number of threads available.
 */

final class CallCenter {
    private final int agents
    private final DataflowQueue queue
    private final DataflowQueue clockOut = new DataflowQueue()
    private final PGroup group = new DefaultPGroup()

    CallCenter(final int agents, final DataflowQueue queue) {
        this.agents = agents
        this.queue = queue
    }

    def open() {
        println "Call center opening"
        agents.times {agentIndex ->
            group.operator(queue, clockOut) {
                if (it == -1) {
                    println "Agent $agentIndex going home"
                    bindOutput true
                    terminate()
                } else {
                    println "Agent $agentIndex answering a call num $it"
                    sleep 100
                    println "Agent $agentIndex answered a call num $it"
                }
            }
            println "Agent $agentIndex logging in"
        }

        println "Call center open"
    }

    def close() {
        println "Call center closing"
        agents.times { queue << -1 }
        agents.times { clockOut.val }
        group.shutdown()
        println "Call center closed"
    }
}

int numberOfCalls = 100
final DataflowQueue incomingCalls = new DataflowQueue()
final CallCenter center = new CallCenter(10, incomingCalls)

final long startTime = System.nanoTime()

center.open()
numberOfCalls.times {
    sleep 10
    incomingCalls << it
}
center.close()

long stopTime = System.nanoTime()
println "Done in ${stopTime = startTime} ms"


