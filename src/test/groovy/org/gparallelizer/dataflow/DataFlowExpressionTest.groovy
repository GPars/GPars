//  GParallelizer
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.gparallelizer.dataflow

import org.gparallelizer.actors.pooledActors.PooledActorGroup
import org.gparallelizer.dataflow.DataFlow
import org.gparallelizer.dataflow.DataFlowStream
import org.gparallelizer.dataflow.DataFlowVariable
import org.gparallelizer.dataflow.operator.DataFlowOperator
import static org.gparallelizer.dataflow.operator.DataFlowOperator.operator

public class DataFlowExpressionTest extends GroovyTestCase {

    public void testInvoke() {
        final DataFlowVariable a = new DataFlowVariable()
        final DataFlowVariable b = new DataFlowVariable()
        final DataFlowVariable c = new DataFlowVariable()

        def d = a * b + c + 1

        shouldFail(MissingMethodException) {
          def e = 1 + a * b + c
        }

        DataFlow.start { c << 40 }
        DataFlow.start { a << 5 }
        DataFlow.start { b << 20 }

        assertEquals 141, d.val
    }
}