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
 * Shows two cooperating agents.
 * Although it would be tempting to use acc1 from within the function processed by a2, it would violate the encapsulation
 * rule of agents and could lead to any of the ugly shared mutable state issues.
 * It is important to ensure that agents do not leak their internal state to the other agents.
 */

def account1 = [withdraw: {println "Withdrawing: $it"}] as Object
def account2 = [deposit: {println "Depositing: $it"}] as Object

def a1 = new Agent(account1)
def a2 = new Agent(account2)

def amount = 20

a1 << {acc1 ->
    acc1.withdraw(amount)
    a2 << {acc2 ->
        //make sure you do not access acc1 from the context of this closure, the code here runs in the context of a2, not a1.
        acc2.deposit(amount)
    }
}

sleep 3000