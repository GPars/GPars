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

package groovyx.gpars.agent

import groovyx.gpars.dataflow.DataflowVariable

/**
 * @author Vaclav Pech
 * Date: June 4, 2010
 */
public class AgentListenerParametersTest extends GroovyTestCase {

    public void testListenerWithAgentParameter() {
        final DataflowVariable result = new DataflowVariable()
        def counter = new Agent(0)
        counter.addListener {agent, o, n -> result << agent}
        counter 20
        assert counter == result.val
    }

    public void testValidatorWithAgentParameter() {
        final DataflowVariable result = new DataflowVariable()
        def counter = new Agent(0)
        counter.addValidator {agent, o, n -> if (n == 10) throw new RuntimeException('test') else result << agent}
        counter 10
        counter.await()
        assert counter.hasErrors()

        counter 20
        assert counter == result.val
    }

    public void testInvalidListenerArguments() {
        def counter = new Agent(0)
        shouldFail(IllegalArgumentException) {
            counter.addListener {oldValue, newValue, foo, bar -> }
        }
        shouldFail(IllegalArgumentException) {
            counter.addListener {oldValue -> }
        }
        shouldFail(IllegalArgumentException) {
            counter.addListener {}
        }
        shouldFail(IllegalArgumentException) {
            counter.addListener {->}
        }
    }

    public void testInvalidValidatorArguments() {
        def counter = new Agent(0)
        shouldFail(IllegalArgumentException) {
            counter.addValidator {oldValue, newValue, foo, bar -> }
        }
        shouldFail(IllegalArgumentException) {
            counter.addValidator {oldValue -> }
        }
        shouldFail(IllegalArgumentException) {
            counter.addValidator {}
        }
        shouldFail(IllegalArgumentException) {
            counter.addValidator {->}
        }
    }

    @SuppressWarnings("GroovyMethodWithMoreThanThreeNegations")
    public void testCloneStrategyWithValidators() {
        def registrations = new Agent([], {it.clone()})
        registrations.addValidator {o, n -> if ('Joe' in n) throw new IllegalArgumentException('Joe must not be allowed to register!')}

        registrations {updateValue(['Joe'])}
        assert registrations.val == []
        assert registrations.hasErrors()
        assert 1 == registrations.errors.size()

        registrations {updateValue(['Dave'])}
        assert registrations.val == ['Dave']
        assert !registrations.hasErrors()

        registrations {updateValue(['Joe'])}
        assert registrations.val == ['Dave']
        assert registrations.hasErrors()
        assert 1 == registrations.errors.size()

        registrations {updateValue(it << 'Joe')}
        assert registrations.val == ['Dave']
        assert registrations.hasErrors()
        assert 1 == registrations.errors.size()

        registrations {r ->
            r << 'Alice'
            r << 'Joe'
            r << 'James'
            updateValue r
        }
        assert registrations.val == ['Dave']
        assert registrations.hasErrors()
        assert 1 == registrations.errors.size()

        registrations {r ->
            r << 'Alice'
            r << 'James'
            updateValue r
        }
        assert registrations.val == ['Dave', 'Alice', 'James']
        assert !registrations.hasErrors()
    }
}