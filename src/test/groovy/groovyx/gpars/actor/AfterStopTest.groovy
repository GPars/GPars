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

package groovyx.gpars.actor

import org.codehaus.groovy.runtime.NullObject

class AfterStopTest extends GroovyTestCase {

    public void testAfterStop() {
        final def actor = new MyAfterStopTestActor2()
        actor.start()
        actor 'Hello'
        assert 5 == actor.sendAndWait(null)

        actor 'die'
        actor.join()

        assert -1 == actor.counter
    }

    public void testAfterStopWithNoArgument() {
        final def actor = new MyAfterStopTestActor1()
        actor.start()
        actor 'Hello'
        assert 5 == actor.sendAndWait(null)

        actor 'die'
        actor.join()

        assert -1 == actor.counter
    }
}

final class MyAfterStopTestActor1 extends DynamicDispatchActor {
    int counter = 0

    void onMessage(String message) {
        if (message == 'die') {
            stop()
        } else {
            counter += message.size()
        }
    }

    void onMessage(NullObject message) {
        reply counter
    }

    void afterStop() {
        counter = -1
    }
}

final class MyAfterStopTestActor2 extends DynamicDispatchActor {
    int counter = 0

    void onMessage(String message) {
        if (message == 'die') {
            stop()
        } else {
            counter += message.size()
        }
    }

    void onMessage(NullObject message) {
        reply counter
    }

    void afterStop(messages) {
        counter = -1
    }
}

