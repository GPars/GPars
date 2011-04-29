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

import groovyx.gpars.stm.GParsStm
import org.multiverse.api.references.IntRef
import static org.multiverse.api.StmUtils.newIntRef

//todo transactional properties to avoid creating getters only to call atomic{}
//todo test exception handling
//todo make sure the multiverse dependency is optional

public class Account {
    private final IntRef amount = newIntRef(0);

    public void transfer(final int a) {
        GParsStm.atomic {
            amount.increment(a);
            println 'Running for ' + a
            sleep 3000
            amount.increment(a);
        }
    }

    public int getCurrentAmount() {
        GParsStm.atomicWithInt {
            amount.get();
        }
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


