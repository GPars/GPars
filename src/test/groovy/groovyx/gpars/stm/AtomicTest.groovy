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

package groovyx.gpars.stm

import java.util.concurrent.CountDownLatch
import org.multiverse.api.AtomicBlock
import org.multiverse.api.PropagationLevel
import org.multiverse.api.references.IntRef
import static org.multiverse.api.StmUtils.newIntRef

/**
 * @author Vaclav Pech
 */
class AtomicTest extends GroovyTestCase {
    public void testAtomicBlock() {
        final Account account = new Account()
        account.transfer(10)
        def t1 = Thread.start {
            account.transfer(100)
        }
        def t2 = Thread.start {
            account.transfer(20)
        }

        [t1, t2]*.join()
        assert 260 == account.currentAmount
    }

    public void testAtomicBlockException() {
        final Account account = new Account()
        account.transfer(10)
        shouldFail(IllegalArgumentException) {
            account.transfer(-1)
        }
        account.transfer(10)

        def t1 = Thread.start {
            account.transfer(100)
            shouldFail(IllegalArgumentException) {
                account.transfer(-1)
            }
            account.transfer(100)
        }
        def t2 = Thread.start {
            account.transfer(20)
        }

        [t1, t2]*.join()
        assert 480 == account.currentAmount
    }

    public void testSingleCustomAtomicBlock() {
        final Account account = new Account()
        final AtomicBlock block = GParsStm.createTxnExecutor(maxRetries: 3000, familyName: 'Custom', PropagationLevel: PropagationLevel.Requires, interruptible: false)
        GParsStm.atomic(block) {
            account.transfer(10)
            def t1 = Thread.start {
                account.transfer(100)
            }
            def t2 = Thread.start {
                account.transfer(20)
            }

            [t1, t2]*.join()
            assert 260 == account.currentAmount
        }
    }

    public void testSingleCustomAtomicBooleanBlock() {
        final AtomicBlock block = GParsStm.createTxnExecutor(maxRetries: 3000, familyName: 'Custom', PropagationLevel: PropagationLevel.Requires, interruptible: false)
        assert GParsStm.atomicWithBoolean(block) {
            true
        }
    }

    public void testSingleCustomAtomicLongBlock() {
        final AtomicBlock block = GParsStm.createTxnExecutor(maxRetries: 3000, familyName: 'Custom', PropagationLevel: PropagationLevel.Requires, interruptible: false)
        assert 10L == GParsStm.atomicWithLong(block) {
            10L
        }
    }

    public void testSingleCustomAtomicIntBlock() {
        final AtomicBlock block = GParsStm.createTxnExecutor(maxRetries: 3000, familyName: 'Custom', PropagationLevel: PropagationLevel.Requires, interruptible: false)
        assert 10 == GParsStm.atomicWithInt(block) {
            10
        }
    }

    public void testSingleCustomAtomicDoubleBlock() {
        final AtomicBlock block = GParsStm.createTxnExecutor(maxRetries: 3000, familyName: 'Custom', PropagationLevel: PropagationLevel.Requires, interruptible: false)
        assert 10.0d == GParsStm.atomicWithDouble(block) {
            10.0d
        }
    }

    public void testCustomAtomicBlock() {
        final Account account = new Account()
        final AtomicBlock block = GParsStm.createTxnExecutor(maxRetries: 3000, familyName: 'Custom', PropagationLevel: PropagationLevel.Requires, interruptible: false)

        final CountDownLatch latch = new CountDownLatch(1)
        def t1 = Thread.start {
            GParsStm.atomic(block) {
                account.transfer(100)
                latch.await()
            }
        }
        GParsStm.atomic(block) {
            account.transfer(10)
            assert 20 == account.currentAmount
        }
        latch.countDown()
        t1.join()
        assert 220 == account.currentAmount
    }

    public void testCustomAtomicBlockWithTimeout() {
        final Account account = new Account()
        final AtomicBlock block = GParsStm.createTxnExecutor(timeoutNs: 1000L, familyName: 'Custom', PropagationLevel: PropagationLevel.Requires, interruptible: false)
        GParsStm.atomic(block) {
            account.transfer(10)
            assert 20 == account.currentAmount
        }
    }

    public void testCustomAtomicBlockWithInvalidParameters() {
        shouldFail(IllegalArgumentException) {
            GParsStm.createTxnExecutor(familyNam: 'Custom')
        }
        shouldFail(IllegalArgumentException) {
            GParsStm.createTxnExecutor('': 'Foo')
        }
    }

    public void testRetry() {
        final AtomicBlock block = GParsStm.createTxnExecutor(maxRetries: 3000, familyName: 'Custom', PropagationLevel: PropagationLevel.Requires, interruptible: false)

        def counter = newIntRef(0)
        final int max = 100
        Thread.start {
            while (counter.atomicGet() < max) {
                counter.atomicIncrementAndGet(1)
                sleep 10
            }
        }
        assert max + 1 == GParsStm.atomicWithInt(block) {tx ->
            if (counter.get() == max) return counter.get() + 1
            tx.retry()
        }
    }

}

public class Account {
    private final IntRef amount = newIntRef(0);

    public void transfer(final int a) {
        GParsStm.atomic {
            amount.increment(a);
            sleep 300
            if (a == -1) throw new IllegalArgumentException('test')
            amount.increment(a);
        }
    }

    public int getCurrentAmount() {
        GParsStm.atomicWithInt {
            amount.get();
        }
    }
}
