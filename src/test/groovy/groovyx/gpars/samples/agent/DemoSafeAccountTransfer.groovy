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
import static groovyx.gpars.GParsPool.withPool

/**
 * For demoing the classical issue of updating the balance of an account and
 * transferring money between accounts concurrently
 * without lost updates or deadlocks.
 * Money transfer is still to be composed of existing, safe credit/debit actions.
 * Unlike accounts in real banks, transferring money is _not_ transactional!
 * @author Dierk König
 */
class Account {
    Agent balance = new Agent(0)

    void credit(int add) {
        balance << { updateValue it + add } // protect against lost updates
    }
    /** This is not transactional!   */
    void transferTo(Account target, int amount) {
        credit(-amount)
        target.credit amount
    }
}

def a = new Account()
def b = new Account()

withPool(50) {
    (1..1000).eachParallel { // while in this scope, other threads may see the total of all accounts as unbalanced.
        a.transferTo b, it   // even mutual transfer does not lead to deadlocks
        b.transferTo a, it
    }                        // all accounts are balanced again.
}
assert [0, 0] == [a, b].balance.val