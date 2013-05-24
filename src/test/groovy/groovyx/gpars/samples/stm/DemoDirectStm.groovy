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

package groovyx.gpars.samples.stm

import org.multiverse.api.Txn
import org.multiverse.api.callables.TxnCallable
import org.multiverse.api.callables.TxnIntCallable
import org.multiverse.api.callables.TxnVoidCallable
import org.multiverse.api.references.TxnInteger
import org.multiverse.api.references.TxnRef

import static org.multiverse.api.StmUtils.atomic
import static org.multiverse.api.StmUtils.newTxnInteger
import static org.multiverse.api.StmUtils.newTxnRef

/**
 * Shows how to call Multiverse directly
 */


public class MyAccount {
    private final TxnInteger amount = newTxnInteger(0);
    final private TxnRef date = newTxnRef(new Date());

    public void transfer(final int a) {
        atomic({Txn tx ->
            amount.increment(a);
            date.set(new Date());
        } as TxnVoidCallable)
    }

    public Date getLastModifiedDate() {
        atomic({Txn tx ->
            date.get();
        } as TxnCallable)
    }

    public int getCurrentAmount() {
        atomic({Txn tx ->
            amount.get();
        } as TxnIntCallable)
    }
}

final MyAccount account = new MyAccount()
account.transfer(10)
def t1 = Thread.start {
    account.transfer(10)
}
def t2 = Thread.start {
    account.transfer(20)
}

[t1, t2]*.join()
println account.currentAmount
println account.lastModifiedDate
