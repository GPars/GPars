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

package groovyx.gpars.samples.dataflow.select

import groovyx.gpars.dataflow.DataflowQueue
import static groovyx.gpars.dataflow.Dataflow.select

/**
 * Shows that Select chooses the input channel randomly.
 */

def a = new DataflowQueue()
def b = new DataflowQueue()

final NUMBER_OF_MESSAGES = 100000
NUMBER_OF_MESSAGES.times {
    a << -1
    b << 1
}

def select = select([a, b])

def balance = 0
def maxDistance = 0

(2 * NUMBER_OF_MESSAGES).times {
    final selectedValue = select.select()
    balance += selectedValue.value
    final double currentDistance = Math.abs(balance)
    if (currentDistance > maxDistance) maxDistance = currentDistance
}

println "The final balance: $balance"
println "The maximum distance: $maxDistance"
println "The maximum distance: ${maxDistance / NUMBER_OF_MESSAGES * 100} % of the maximal possible distance"