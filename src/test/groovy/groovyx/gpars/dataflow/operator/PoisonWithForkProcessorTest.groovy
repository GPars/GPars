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

package groovyx.gpars.dataflow.operator

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.group.PGroup
import java.util.concurrent.CyclicBarrier

/**
 * @author Vaclav Pech
 */
class PoisonWithForkProcessorTest extends GroovyTestCase {
    private PGroup group
    final DataflowQueue a = new DataflowQueue()
    final DataflowQueue b = new DataflowQueue()
    final DataflowQueue c = new DataflowQueue()

    protected void setUp() {
        group = new DefaultPGroup(20)
        super.setUp()
    }

    protected void tearDown() {
        group.shutdown()
        super.tearDown()
    }

    public void testPoisonWithSequentialWriteIntoChannels() {
        def op = group.operator(inputs: [a, b], outputs: [c], maxForks: 4) {x, y ->
            bindOutput x + y
        }
        100.times {
            a << 10
        }

        100.times {
            b << 20
        }
        a << PoisonPill.instance
        100.times {
            assert 30 == c.val
        }
        assert PoisonPill.instance == c.val
        op.join()
    }

    public void testPoisonWithParallelWriteIntoChannels() {
        def op = group.operator(inputs: [a, b], outputs: [c], maxForks: 4) {x, y ->
            bindOutput x + y
        }
        final barrier = new CyclicBarrier(2)
        final t1 = Thread.start {
            barrier.await()
            100.times {
                a << 10
            }
            a << PoisonPill.instance
        }

        final t2 = Thread.start {
            barrier.await()
            100.times {
                b << 20
            }
            b << PoisonPill.instance
        }

        [t1, t2]*.join()
        a << PoisonPill.instance
        100.times {
            assert 30 == c.val
        }
        assert PoisonPill.instance == c.val
        op.join()
    }
}
