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

package groovyx.gpars.dataflow.operator

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.group.NonDaemonPGroup
import groovyx.gpars.group.PGroup
import org.codehaus.groovy.runtime.NullObject

/**
 * @author Vaclav Pech
 */
public class PipelineFilterTest extends GroovyTestCase {

    private PGroup group

    public void setUp() throws Exception {
        group = new NonDaemonPGroup()
    }

    public void tearDown() throws Exception {
        group.shutdown()
    }

    public void testPipelineTransformation() {
        final DataflowQueue queue1 = new DataflowQueue()
        final DataflowQueue queue2 = new DataflowQueue()

        final odd = {num -> num % 2 != 0}

        new Pipeline(group, queue1).chainWith odd into queue2
        (1..5).each {queue1 << it}
        assert [true, false, true, false, true] == (1..5).collect {queue2.val}
    }

    public void testPipelineNullObjectBlocking() {
        final DataflowQueue queue1 = new DataflowQueue()
        final DataflowQueue queue2 = new DataflowQueue()

        final odd = {num ->
            if (num == 5) return null  //null values are normally passed on
            if (num % 2 != 0) return num
            else return NullObject.nullObject  //this value gets blocked
        }

        new Pipeline(group, queue1).chainWith odd into queue2
        (1..5).each {queue1 << it}
        assert 1 == queue2.val
        assert 3 == queue2.val
        assert null == queue2.val
    }

    public void testPipelineVoidResult() {
        final DataflowQueue queue1 = new DataflowQueue()
        final DataflowQueue queue2 = new DataflowQueue()

        final odd = {num -> }

        new Pipeline(group, queue1).chainWith odd into queue2
        queue1 << 1
        queue1 << 2
        assert null == queue2.val
        assert null == queue2.val
    }

    public void testPipelineFilter() {
        final DataflowQueue queue1 = new DataflowQueue()
        final DataflowQueue queue2 = new DataflowQueue()

        final odd = {num -> num % 2 != 0 }

        new Pipeline(group, queue1).filter odd into queue2
        (1..5).each {queue1 << it}
        assert 1 == queue2.val
        assert 3 == queue2.val
        assert 5 == queue2.val
    }
}
