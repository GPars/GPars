//  GPars (formerly GParallelizer)
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

package groovyx.gpars.dataflow

/**
 * @author Alex Tkachman
 */
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

    public void testProperty() {
        final DataFlowVariable a = new DataFlowVariable()
        final DataFlowVariable b = new DataFlowVariable()

        def prod = a.x * b.x + a.y * b.y + a.z * b.z

        DataFlow.start {
            a << [x: 3, y: 2, z: 1]
            b << [x: 1, y: 2, z: 3]
        }

        assertEquals 11, (prod + 1).val
    }

    public void testTransform() {
        final DataFlowVariable a = new DataFlowVariable()
        final DataFlowVariable b = new DataFlowVariable()

        def prod = DataFlowExpression.transform([a, b]) {x, y ->
            x + y
        }

        DataFlow.start {
            a << 5
            b << 7
        }

        assertEquals(13, (prod + 1).val)

        shouldFail(IllegalArgumentException) {
            DataFlowExpression.transform([a]) {x, y ->}
        }

        shouldFail(IllegalArgumentException) {
            DataFlowExpression.transform([a, b, null]) {x, y ->}
        }

        shouldFail(IllegalArgumentException) {
            DataFlowExpression.transform([a]) {->}
        }
    }
}