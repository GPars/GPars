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

import groovyx.gpars.dataflow.Dataflows

/**
 * @author Vaclav Pech
 * Date: June 4, 2010
 */
public class AgentListenerTest extends GroovyTestCase {
    public void testUpdateListeners() {
        final Dataflows flows = new Dataflows()
        def counter = new Agent(0)
        counter.addListener {oldValue, newValue -> flows.old1 = oldValue; flows.new1 = newValue}
        counter.addListener {oldValue, newValue -> flows.old2 = oldValue; flows.new2 = newValue}

        counter {updateValue(it + 1)}
        counter.await()
        assert 1 == counter.instantVal
        assert flows.old1 == flows.old2
        assert flows.old1 == 0
        assert flows.new1 == flows.new2
        assert flows.new1 == 1
    }

    public void testValidators() {
        def counter = new Agent(0)
        counter {updateValue(it + 1)}
        assert 1 == counter.val

        counter.addValidator {oldValue, newValue -> if (newValue < oldValue) throw new IllegalArgumentException('Decrease is not allowed!')}
        counter {updateValue(it + 1)}
        assert 2 == counter.val

        counter {updateValue(it)}
        assert 2 == counter.val

        counter {updateValue(it - 1)}
        assert 2 == counter.val

        counter {updateValue(it + 3)}
        assert 5 == counter.val

        counter.addValidator {oldValue, newValue -> if (newValue > oldValue + 1) throw new IllegalArgumentException('Jumps ahead are prohibited!')}

        counter {updateValue(it - 1)}
        assert 5 == counter.val

        counter {updateValue(it + 3)}
        assert 5 == counter.val

        counter {updateValue(it + 1)}
        assert 6 == counter.val
    }

    public void testListenerThrowingException() {
        def counter = new Agent(0)
        counter {updateValue(it + 1)}
        assert 1 == counter.val

        counter.addListener {oldValue, newValue -> if (newValue < oldValue) throw new IllegalArgumentException('Decrease is not allowed! But hey, I\'m just a listener!')}
        counter {updateValue(it + 1)}
        assert 2 == counter.val
        assert !counter.hasErrors()
        assert 0 == counter.errors.size()

        counter {updateValue(it - 1)}
        assert 1 == counter.val
        assert counter.hasErrors()
        assert 1 == counter.errors.size()

        counter {updateValue(it + 3)}
        assert 4 == counter.val
        assert !counter.hasErrors()

        counter.addValidator {oldValue, newValue -> if (newValue > oldValue + 1) throw new IllegalArgumentException('Jumps ahead are prohibited!')}

        counter {updateValue(it - 1)}
        assert 3 == counter.val
        assert counter.hasErrors()
        assert 1 == counter.errors.size()

        counter {updateValue(it + 3)}
        assert 3 == counter.val
        assert counter.hasErrors()
        assert 1 == counter.errors.size()
    }
}