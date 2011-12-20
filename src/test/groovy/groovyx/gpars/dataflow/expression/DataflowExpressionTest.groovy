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

package groovyx.gpars.dataflow.expression

import groovyx.gpars.GParsPool
import groovyx.gpars.dataflow.Dataflow
import groovyx.gpars.dataflow.DataflowVariable

/**
 * @author Alex Tkachman
 */
public class DataflowExpressionTest extends GroovyTestCase {

    public void testInvoke() {
        final DataflowVariable a = new DataflowVariable()
        final DataflowVariable b = new DataflowVariable()
        final DataflowVariable c = new DataflowVariable()

        def d = a * b + c + 1

        shouldFail(MissingMethodException) {
            def e = 1 + a * b + c
        }

        Dataflow.task { c << 40 }
        Dataflow.task { a << 5 }
        Dataflow.task { b << 20 }

        assert 141 == d.val
    }

    public void testProperty() {
        final DataflowVariable a = new DataflowVariable()
        final DataflowVariable b = new DataflowVariable()

        def prod = a.x * b.x + a.y * b.y + a.z * b.z

        Dataflow.task {
            a << [x: 3, y: 2, z: 1]
            b << [x: 1, y: 2, z: 3]
        }

        assert 11 == (prod + 1).val
    }

    public void testTransform() {
        final DataflowVariable a = new DataflowVariable()
        final DataflowVariable b = new DataflowVariable()

        def prod = DataflowExpression.transform([a, b]) {x, y ->
            x + y
        }

        Dataflow.task {
            a << 5
            b << 7
        }

        assert 13 == (prod + 1).val

        shouldFail(IllegalArgumentException) {
            DataflowExpression.transform([a]) {x, y ->}
        }

        shouldFail(IllegalArgumentException) {
            DataflowExpression.transform([a, b, null]) {x, y ->}
        }

        shouldFail(IllegalArgumentException) {
            DataflowExpression.transform([a]) {->}
        }
    }

    public void testTransformWithAsyncFun() {
        final DataflowVariable a = new DataflowVariable()
        final DataflowVariable b = new DataflowVariable()

        GParsPool.withPool {
            final Closure transformation = {x, y ->
                x + y
            }.asyncFun()

            def prod = transformation(a, b)

            Dataflow.task {
                a << 5
                b << 7
            }

            assert 13 == (prod + 1).val
        }
    }
}