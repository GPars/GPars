// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
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

import groovyx.gpars.dataflow.DataFlowChannel
import groovyx.gpars.dataflow.DataFlowVariable
import groovyx.gpars.group.DefaultPGroup
import static groovyx.gpars.dataflow.DataFlow.task

class StreamAsDataFlowChannelTest extends GroovyTestCase {

    public void testInterfaceImplemented() {
        def stream = new DataFlowStreamAdapter(new DataFlowStream())
        assert stream instanceof DataFlowChannel
    }

    public void testGetVal() {
        def stream = new DataFlowStreamAdapter(new DataFlowStream())
        task {
            stream << 1
        }
        assert stream.val == 1
    }

    public void testRightShift() {
        def stream = new DataFlowStreamAdapter(new DataFlowStream())
        def result = new DataFlowVariable()
        stream >> {result << it}
        task {
            stream << 1
        }
        assert result.val == 1
    }

    public void testWhenBound() {
        def stream = new DataFlowStreamAdapter()
        def result = new DataFlowVariable()
        stream.whenBound() {result << it}
        new DefaultPGroup().task {
            stream << 1
        }
        assert result.val == 1
    }
}
