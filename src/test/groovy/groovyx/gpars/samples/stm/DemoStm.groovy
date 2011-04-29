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

package groovyx.gpars.samples.stm

import org.multiverse.api.Transaction
import org.multiverse.api.closures.AtomicIntClosure
import org.multiverse.api.closures.AtomicVoidClosure
import org.multiverse.api.references.IntRef
import static org.multiverse.api.StmUtils.execute
import static org.multiverse.api.StmUtils.newIntRef

//todo add multiverse license.txt to the lib folder
//todo transactional properties to avoit creating getters only to call atomic{}

public class Account {
    private final IntRef amount = newIntRef(0);

    public void transfer(final int a) {
        execute({Transaction tx ->
            amount.increment(a);
            println 'Running for ' + a
            sleep 3000
            amount.increment(a);
        } as AtomicVoidClosure)
    }

    public int getCurrentAmount() {
        execute({Transaction tx ->
            return amount.get();
        } as AtomicIntClosure)
    }
}

final Account account = new Account()
account.transfer(10)
def t1 = Thread.start {
    account.transfer(2)
    account.transfer(3)
}
def t2 = Thread.start {
    account.transfer(20)
    account.transfer(30)
}

[t1, t2]*.join()
println account.currentAmount
