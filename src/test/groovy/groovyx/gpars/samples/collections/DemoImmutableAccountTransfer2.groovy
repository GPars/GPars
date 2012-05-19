// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2012  The original author or authors
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

package groovyx.gpars.samples.collections

import static groovyx.gpars.GParsPool.withPool
import groovy.transform.Immutable

/**
 * Demos another possible use of Groovy immutable support for the classical
 * account money transfer problem.
 * Account becomes immutable without knowing anything about synchronization.
 * However, since transferTo() returns two new Accounts, these references
 * must be assigned in one atomic action. This is what ListSafe cares for.
 * It also supports an atomic get-and-set operation for updating the
 * reference list safely.
 * Note that this puts the parallel transfer calls in a strict,
 * yet not foreseeable sequence. It effectively disables concurrent transfers.
 * This may still be valuable since it
 * reliefs the account from knowing about concurrency.
 * @author Dierk König
 */
@Immutable class ImmutableAccount2 {
    int balance = 0

    ImmutableAccount2 credit(int add) {
        new ImmutableAccount2(balance + add)
    }

    /** @return list of new "this" and new target account   */
    List<ImmutableAccount2> transferTo(ImmutableAccount2 target, int amount) {
        [credit(-amount), target.credit(amount)]
    }
}

class ReferenceSafe {
    private List references

    synchronized List getValues() { references }
    /** @param update a closure that gets called with the spread of saved refs
     * and that's return value becomes the new reference list  */
    synchronized void setValues(Closure update) {
        references = update(* references).asImmutable()
    }
}

def accounts = new ReferenceSafe()
accounts.values = { [new ImmutableAccount2(0), new ImmutableAccount2(0)] }

withPool(50) {
    (1..1000).eachParallel {
        accounts.values = { a, b -> a.transferTo b, 1 }
    }
}
assert [-1000, 1000] == accounts.values.balance