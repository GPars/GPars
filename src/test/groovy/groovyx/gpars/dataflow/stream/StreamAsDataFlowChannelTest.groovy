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

package groovyx.gpars.dataflow.stream

import groovyx.gpars.dataflow.DataflowReadChannel
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.group.DefaultPGroup
import static groovyx.gpars.dataflow.Dataflow.task

class StreamAsDataflowChannelTest extends GroovyTestCase {

    public void testInterfaceImplemented() {
        def stream = new DataflowStreamReadAdapter(new DataflowStream())
        assert stream instanceof DataflowReadChannel
    }

    public void testGetVal() {
        final DataflowStream original = new DataflowStream()
        def stream = new DataflowStreamReadAdapter(original)
        task {
            original << 1
        }
        assert stream.val == 1
    }

    public void testRightShift() {
        final DataflowStream original = new DataflowStream()
        def stream = new DataflowStreamReadAdapter(original)
        def result = new DataflowVariable()
        stream >> {result << it}
        task {
            original << 1
        }
        assert result.val == 1
    }

    public void testWhenBound() {
        def original = new DataflowStream()
        def stream = new DataflowStreamReadAdapter(original)
        def result = new DataflowVariable()
        stream.whenBound() {result << it}
        new DefaultPGroup().task {
            original << 1
        }
        assert result.val == 1
    }
}
