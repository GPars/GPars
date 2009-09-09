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

package org.gparallelizer.dataflow.operator

import org.gparallelizer.dataflow.DataFlowVariable
import org.gparallelizer.dataflow.DataFlowStream
import static org.gparallelizer.dataflow.operator.DFOperator.*
import org.gparallelizer.dataflow.DataFlow

/**
* @author Vaclav Pech
 * Date: Sep 9, 2009
 */

public class OperatorTest extends GroovyTestCase {

    public void testOperator() {
        final DataFlowVariable a = new DataFlowVariable()
        final DataFlowVariable b = new DataFlowVariable()
        final DataFlowStream c = new DataFlowStream()
        final DataFlowVariable d = new DataFlowVariable()
        final DataFlowStream e = new DataFlowStream()

        def op = operator(inputs : [a, b, c], outputs : [d, e]) {x, y, z ->
            bindOutput 0, x + y + z
            bindOutput 1, x * y * z
        }

        DataFlow.start { a << 5 }
        DataFlow.start { b << 20 }
        DataFlow.start { c << 40 }

        assertEquals 65, d.val
        assertEquals 4000, e.val

        op.stop()
    }

    public void testCombinedOperators() {
        final DataFlowStream a = new DataFlowStream()
        final DataFlowStream b = new DataFlowStream()
        final DataFlowStream c = new DataFlowStream()
        a << 1
        a << 2
        a << 3
        b << 4
        b << 5
        b << 6
        b << 7

        final DataFlowStream x = new DataFlowStream()
        def op1 = operator(inputs : [a], outputs : [x]) {v ->
            bindOutput 0, v * v
        }

        final DataFlowStream y = new DataFlowStream()
        def op2 = operator(inputs : [b], outputs : [y]) {v ->
            bindOutput 0, v * v
        }

        def op3 = operator(inputs : [x, y], outputs : [c]) {v1, v2 ->
            bindOutput 0, v1 + v2
        }

        assertEquals 17, c.val
        assertEquals 29, c.val
        assertEquals 45, c.val
        [op1, op2, op3]*.stop()
    }

}