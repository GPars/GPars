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

package groovyx.gpars.dataflow

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import static groovyx.gpars.actor.Actors.actor

public class SyncDataflowVariableTest extends GroovyTestCase {

    public void testWriterBlocking() {
        final SyncDataflowVariable variable = new SyncDataflowVariable(1)
        volatile boolean reached = false

        def t = Thread.start {
            variable << 10
            reached = true
        }
        sleep 1000
        assert !reached
        assertEquals 10, variable.val
        t.join()
        assertEquals 10, variable.val
        assertEquals 10, variable.val
        assertEquals 10, variable.val

        assert reached
    }

    public void testReaderBlocking() {
        final SyncDataflowVariable variable = new SyncDataflowVariable(2)
        volatile boolean writerReached = false
        volatile int readerReached = 0

        def t1 = Thread.start {
            variable << 10
            writerReached = true
        }
        def t2 = Thread.start {
            readerReached = variable.val
        }
        sleep 1000
        assert !writerReached
        assert readerReached == 0
        assertEquals 10, variable.val
        [t1, t2]*.join()
        assert writerReached
        assert readerReached == 10
    }

    public void testGet() {
        final SyncDataflowVariable variable = new SyncDataflowVariable(1)
        Thread.start {variable << 10}
        assertEquals 10, variable.get()
    }

    public void testMultiGet() {
        final SyncDataflowVariable variable = new SyncDataflowVariable(3)
        Thread.start {variable << 10}
        Thread.start {assertEquals 10, variable.get()}
        Thread.start {assertEquals 10, variable.get()}
        Thread.start {assertEquals 10, variable.get(10, TimeUnit.SECONDS)}
    }

    public void testTimeoutGet() {
        final SyncDataflowVariable variable = new SyncDataflowVariable(1)
        shouldFail(TimeoutException) {
            variable.get(1, TimeUnit.SECONDS)
        }
        Thread.start {variable << 10}
        assertEquals 10, variable.get(10, TimeUnit.SECONDS)
    }

    public void testAsyncRead() {
        final SyncDataflowVariable variable = new SyncDataflowVariable(2)
        def result1 = new DataflowVariable()
        def actor = actor {
            react {
                result1 << it
            }
        }

        Thread.start {
            variable.getValAsync(actor)
        }

        def result2 = new DataflowVariable()
        Thread.start {
            variable.whenBound({result2 << it})
        }

        Thread.start {variable << 10}

        assertEquals 10, result1.val
        assertEquals 10, result2.val
    }
}
