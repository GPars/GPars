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
import groovyx.gpars.dataflow.DataFlowVariable
import groovyx.gpars.dataflow.DataFlows
import groovyx.gpars.group.DefaultPGroup

/**
 * @author Vaclav Pech
 */
class ParallelGroupTest extends GroovyTestCase {
    public void testParallelGroup() {
        final DefaultPGroup group = new DefaultPGroup()
        final DataFlows df = new DataFlows()
        def actor = group.actor {
            df.group1 = parallelGroup
            react {}
        }
        actor.sendAndContinue(10) {
            df.group2 = parallelGroup
        }

        assert df.group1.is(df.group1)
    }

    public void _testDataflowContinuations() {
        final DataFlowVariable variable = new DataFlowVariable()
        final DataFlows df = new DataFlows()
        DataFlow.task {
            df.group1 = activeParallelGroup()
            variable.whenBound {}
        }
        assert df.group1 != null
    }
}
