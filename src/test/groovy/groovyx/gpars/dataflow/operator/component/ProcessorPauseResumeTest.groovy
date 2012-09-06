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

package groovyx.gpars.dataflow.operator.component

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.group.PGroup

/**
 * @author Vaclav Pech
 */
public class ProcessorPauseResumeTest extends GroovyTestCase {
    private PGroup group
    final DataflowQueue a = new DataflowQueue()
    final DataflowQueue b = new DataflowQueue()
    final DataflowQueue c = new DataflowQueue()

    protected void setUp() {
        group = new DefaultPGroup(1)
        super.setUp()
    }

    protected void tearDown() {
        group.shutdown()
        super.tearDown()
    }


    public void testPause() throws Exception {
        final listener = new ProcessorPauseResume()
        def op = group.operator(inputs: [a, b], outputs: [c], listeners: [listener]) {x, y ->
            bindOutput x + y
        }

        a << 10
        b << 20
        assert 30 == c.val

        listener.pause()
        a << 11
        b << 21
        sleep(500)
        assert !c.bound

        a << 12
        b << 22
        sleep(500)
        assert !c.bound

        listener.resume()
        assert 32 == c.val
        assert 34 == c.val

        op.terminate()
    }
}
