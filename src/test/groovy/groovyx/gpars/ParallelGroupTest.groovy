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

package groovyx.gpars

import groovyx.gpars.dataflow.DataFlow
import groovyx.gpars.dataflow.DataFlowExpression
import groovyx.gpars.dataflow.DataFlowVariable
import groovyx.gpars.dataflow.DataFlows
import groovyx.gpars.group.DefaultPGroup

/**
 * @author Vaclav Pech
 */
class ParallelGroupTest extends GroovyTestCase {
    public void testParallelGroup() {
        final DefaultPGroup group = new DefaultPGroup()
        final DataFlows results = new DataFlows()
        def actor = group.actor {
            results.group1 = parallelGroup
            react {}
        }
        actor.sendAndContinue(10) {
            results.group2 = parallelGroup
        }

        assert results.group1.is(results.group1)
    }

    public void testDataflowContinuations() {
        final DataFlowVariable variable = new DataFlowVariable()
        final DataFlows results = new DataFlows()
        DataFlow.task {
            results.group1 = DataFlowExpression.activeParallelGroup.get()
            variable.whenBound {
                results.group2 = DataFlowExpression.activeParallelGroup.get()
            }
            variable << 'Foo'
        }
        assert results.group1 == results.group2
        assert results.group1 == DataFlow.DATA_FLOW_GROUP
    }

    public void testDataflowContinuationsWithCustomGroup() {
        final DataFlowVariable variable = new DataFlowVariable()
        final DataFlows results = new DataFlows()

        final DefaultPGroup group = new DefaultPGroup()
        group.task {
            results.group1 = DataFlowExpression.activeParallelGroup.get()
            variable.whenBound {
                results.group2 = DataFlowExpression.activeParallelGroup.get()
            }
            variable << 'Foo'
        }
        assert results.group1 == results.group2
        assert results.group1 == group
    }

    public void testDataflowContinuationsWithSingleThread() {
        final DataFlowVariable variable = new DataFlowVariable()
        final DataFlows results = new DataFlows()

        final DefaultPGroup group = new DefaultPGroup(1)
        group.task {
            results.t1 = Thread.currentThread()
            variable.whenBound {
                results.t2 = Thread.currentThread()
            }
            variable.whenBound {
                results.t3 = Thread.currentThread()
            }
            variable.whenBound {
                results.t4 = Thread.currentThread()
            }
            variable << 'Foo'
        }
        assert results.t1 == results.t2
        assert (1..4).collect {results.t1} == [results.t1, results.t2, results.t3, results.t4]
    }

    public void testSendAndContinue() {
        final DefaultPGroup group = new DefaultPGroup(1)
        def results = new DataFlows()

        def actor = group.actor {
            results.t1 = Thread.currentThread()
            react {
                results.t2 = Thread.currentThread()
                reply it
            }
        }
        actor.sendAndContinue(1) {results.t3 = Thread.currentThread();}
        assert results.t1 == results.t2
        assert results.t1 == results.t3
    }
}
