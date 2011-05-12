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

import groovyx.gpars.dataflow.Dataflows

class DDAReplyTest extends GroovyTestCase {
    public void testSender() {
        final Dataflows df = new Dataflows()

        def dda = new DynamicDispatchActor().become {
            when {msg ->
                df.sender = sender
                sender.send 10
                reply 20
            }
        }.start()
        def actor = Actors.actor {
            dda << new Object()
            react {
                df.reply1 = it
                react {
                    df.reply2 = it
                }
            }
        }
        assert df.sender == actor
        assert df.reply1 == 10
        assert df.reply2 == 20
    }
}
