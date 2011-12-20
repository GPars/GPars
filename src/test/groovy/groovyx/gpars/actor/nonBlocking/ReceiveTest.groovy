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

package groovyx.gpars.actor.nonBlocking

import groovyx.gpars.actor.Actors
import groovyx.gpars.dataflow.Dataflows
import java.util.concurrent.CyclicBarrier

/**
 * @author Vaclav Pech
 * Date: Sep 14, 2009
 */
public class ReceiveTest extends GroovyTestCase {

    public void testReceive() {
        final Dataflows df = new Dataflows()

        def actor = Actors.blockingActor {
            df.result1 = receive()
            receive {
                df.result2 = it
            }
            receive {
                df.result3 = it
            }
        }

        actor << 'message1'
        actor << 'message2'
        actor << 'message3'
        assert 'message1' == df.result1
        assert 'message2' == df.result2
        assert 'message3' == df.result3
    }

    public void testNestedReceive() {
        final Dataflows df = new Dataflows()

        def actor = Actors.blockingActor {
            while (true) {
                receive {
                    receive {
                        receive {msg1 ->
                            df.result1 = msg1
                            df.result2 = receive()
                            receive {msg2 ->
                                df.result3 = msg2
                                receive {msg3 ->
                                    df.result4 = msg3
                                    terminate()
                                }
                                df.result5 = 'message5'
                            }

                        }
                    }
                }
            }
        }

        actor << 'message'
        actor << 'message'
        actor << 'message1'
        actor << 'message2'
        actor << 'message3'
        actor << 'message4'
        assert 'message1' == df.result1
        assert 'message2' == df.result2
        assert 'message3' == df.result3
        assert 'message4' == df.result4
        assert !df.contains('result5')
    }

    public void testReceiveInLoops() {
        final Dataflows df = new Dataflows()
        final def barrier = new CyclicBarrier(2)

        def actor = Actors.blockingActor {
            while (true) {
                if (df.contains('result1')) {
                    terminate()
                }
                df.result1 = receive()
                receive {
                    df.result2 = it
                }
                barrier.await()
            }
        }

        actor << 'message1'
        actor << 'message2'
        final def msg = new String('message3')
        msg.metaClass.onDeliveryError = {
            df.deliveryError = 'Not delivered'
        }
        actor << msg
        barrier.await()
        assert 'message1' == df.result1
        assert 'message2' == df.result2
        assert 'Not delivered' == df.deliveryError
    }


}