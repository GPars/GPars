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

package groovyx.gpars.samples.agent

import groovyx.gpars.agent.Agent

/**
 * Shows the options to set listeners and validators on agents as closures with 2 or 3 arguments. 
 */

final Agent counter = new Agent()

counter.addListener {oldValue, newValue -> println "Changing value from $oldValue to $newValue"}
counter.addListener {agent, oldValue, newValue -> println "Agent $agent changing value from $oldValue to $newValue"}

counter.addValidator {oldValue, newValue -> if (oldValue > newValue) throw new IllegalArgumentException('Things can only go up in Groovy')}
counter.addValidator {agent, oldValue, newValue -> if (oldValue == newValue) throw new IllegalArgumentException('Things never stay the same for $agent')}

counter 10
counter 11
counter {updateValue 12}
counter 10  //Will be rejected
counter {updateValue it - 1}  //Will be rejected
counter {updateValue it}  //Will be rejected
counter {updateValue 11}  //Will be rejected
counter 12  //Will be rejected
counter 20
counter.await()

assert counter.hasErrors()
assert counter.errors.size() == 5
