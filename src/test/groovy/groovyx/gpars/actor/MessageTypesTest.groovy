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

import static groovyx.gpars.actor.Actors.reactor

public class MessageTypesTest extends GroovyTestCase {

    void testMessages() {
        [
                'normal string message',
                new Object(),
                1,
                1.5d,
                1.5,
                [1, 2, 3],
                [],
                [foo: 1, bar: 2],
                [:]
        ].each { message ->
            def gotClass = null
            def actor = reactor { gotClass = it.class }
            actor.sendAndWait message
            assert message.class == gotClass: "Failed get the proper class for $message"
            actor.stop()
        }
    }

}
