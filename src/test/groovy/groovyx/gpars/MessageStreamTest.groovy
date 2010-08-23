// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
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

package groovyx.gpars

import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.impl.MessageStream
import groovyx.gpars.actor.impl.MessageStream.ResultWaiter
import java.util.concurrent.TimeUnit

/**
 * @author Vaclav Pech, Dierk Koenig
 */
class MessageStreamTest extends GroovyTestCase {

    public void testResultWaiterWithTimeoutAndException() {
        final def waiter = new ResultWaiter()
        waiter.send new RuntimeException('test')
        shouldFail(RuntimeException) {
            waiter.result
        }
        shouldFail(RuntimeException) {
            waiter.getResult(10, TimeUnit.MILLISECONDS)
        }
    }

    void testSendWithoutArguments() {
        def called = false
        def stream = [send: {msg -> called = true; null }] as MessageStream
        stream.send()
        assert called
    }

    public void testForward() {
        volatile def flag1 = false
        volatile def flag2 = false
        volatile def flag3 = false

        def receiver = Actors.actor {
            react {
                flag1 = true
                react {
                    flag2 = true
                    react {
                        flag3 = true
                    }
                }
            }
        }

        def processor = Actors.actor {
            react {
                reply '1'
                it.reply '2'
                it.sender.send '3'
            }
        }

        processor.send('0', receiver)
        receiver.join()
        assert flag1 && flag2 && flag3
    }
}
