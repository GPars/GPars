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

package groovyx.gpars.dataflow

import groovyx.gpars.activeobject.ActiveMethod
import groovyx.gpars.activeobject.ActiveObject
import static groovyx.gpars.GParsPool.withPool

/**
 * Created by IntelliJ IDEA.
 * User: Vaclav
 * Date: 24.11.11
 * Time: 12:15
 * To change this template use File | Settings | File Templates.
 */
class WhenBoundChainingTest extends GroovyTestCase {
    public void testBasicChaining() {
        final DataflowVariable variable = new DataflowVariable()
        final DataflowVariable result = new DataflowVariable()

        variable.then {it * 2}.then {it + 1}.then {result << it}
        variable << 4
        assert 9 == result.val
    }

    public void testBasicChainingWithRightShift() {
        final DataflowVariable variable = new DataflowVariable()
        final DataflowVariable result = new DataflowVariable()

        variable >> {it * 2} >> {it + 1} >> {result << it}
        variable << 4
        assert 9 == result.val
    }

    public void testNestedChaining() {
        final DataflowVariable variable = new DataflowVariable()
        final DataflowVariable result = new DataflowVariable()

        variable.then {
            def a = new DataflowVariable()
            a << it * 2
            return a
        }.then {it + 1}.then {result << it}
        variable << 4
        assert 9 == result.val
    }

    public void testNestedChainingWithRightShift() {
        final DataflowVariable variable = new DataflowVariable()
        final DataflowVariable result = new DataflowVariable()

        variable >> {
            def a = new DataflowVariable()
            a << it * 2
            return a
        } >> {it + 1} >> {result << it}
        variable << 4
        assert 9 == result.val
    }

    public void testFunctionChaining() {
        final DataflowVariable variable = new DataflowVariable()
        final DataflowVariable result = new DataflowVariable()

        final doubler = {it * 2}
        final adder = {it + 1}

        variable.then doubler then adder then {result << it}
        variable << 4
        assert 9 == result.val
    }

    public void testFunctionChainingWithRightShift() {
        final DataflowVariable variable = new DataflowVariable()
        final DataflowVariable result = new DataflowVariable()

        final doubler = {it * 2}
        final adder = {it + 1}

        variable >> doubler >> adder >> {result << it}
        variable << 4
        assert 9 == result.val
    }

    public void testAsyncFunctionChaining() {
        final DataflowVariable variable = new DataflowVariable()
        final DataflowVariable result = new DataflowVariable()

        withPool {
            final doubler = {it * 2}.asyncFun()
            final adder = {it + 1}.asyncFun()

            variable.then doubler then adder then {result << it}
            variable << 4
            assert 9 == result.val
        }
    }

    public void testAsyncFunctionChainingWithRightShift() {
        final DataflowVariable variable = new DataflowVariable()
        final DataflowVariable result = new DataflowVariable()

        withPool {
            final doubler = {it * 2}.asyncFun()
            final adder = {it + 1}.asyncFun()

            variable >> doubler >> adder >> {result << it}
            variable << 4
            assert 9 == result.val
        }
    }

    public void testActiveObjectChaining() {
        final DataflowVariable result = new DataflowVariable()
        final calculator = new ActiveCalculator();
        calculator.doubler(4).then {calculator.adder it}.then {result << it}
        assert 9 == result.val
    }

    public void testActiveObjectChainingWithRightShift() {
        final DataflowVariable result = new DataflowVariable()
        final calculator = new ActiveCalculator();
        calculator.doubler(4) >> {calculator.adder it} >> {result << it}
        assert 9 == result.val
    }

    public void testBasicChainingWithNullValue() {
        final DataflowVariable variable = new DataflowVariable()
        final DataflowVariable result = new DataflowVariable()

        variable.then {it}.then {it}.then {result << it}
        variable << null
        assert null == result.val
    }

    public void testBasicChainingWithNullValueWithRightShift() {
        final DataflowVariable variable = new DataflowVariable()
        final DataflowVariable result = new DataflowVariable()

        variable >> {it} >> {it} >> {result << it}
        variable << null
        assert null == result.val
    }
}

@ActiveObject
class ActiveCalculator {
    @ActiveMethod
    def doubler(int value) {
        value * 2
    }

    @ActiveMethod
    def adder(int value) {
        value + 1
    }

}