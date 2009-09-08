//  GParallelizer
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

package org.gparallelizer.actors.pooledActors

import java.util.concurrent.CountDownLatch

public class FastSendTest extends GroovyTestCase {

    public void testFastSend() {
        volatile Exception exception1, exception2

        final CountDownLatch latch = new CountDownLatch(1)
        final CountDownLatch replyLatch = new CountDownLatch(1)

        final AbstractPooledActor actor = PooledActors.actor {
            disableSendingReplies()
            react {
                try {
                    reply 'Message22'
                } catch (Exception e) {
                    exception1 = e
                }
                try {
                    it.reply 'Message23'
                } catch (Exception e) {
                    exception2 = e
                }
                enableSendingReplies()
                replyLatch.countDown()
                react {
                    replyIfExists 'Message'
                    it.replyIfExists 'Message'
                    latch.countDown()
                }
            }
        }.start()

        actor << 'Message21'
        replyLatch.await()
        actor << 'Enabled message'
        latch.await()
        assertNotNull exception1
        assert exception1 instanceof IllegalStateException
        assertNotNull exception2
        assert exception2 instanceof MissingMethodException
    }

    public void testFastSendFromActor() {
        volatile Exception exception1, exception2

        final CountDownLatch latch = new CountDownLatch(1)
        final CountDownLatch replyLatch = new CountDownLatch(1)

        final AbstractPooledActor actor = PooledActors.actor {
            disableSendingReplies()

            react {
                try {
                    reply 'Message32'
                } catch (Exception e) {
                    exception1 = e
                }
                try {
                    it.reply 'Message33'
                } catch (Exception e) {
                    exception2 = e
                }
                enableSendingReplies()
                replyLatch.countDown()
                react {
                    replyIfExists 'Message'
                    it.replyIfExists 'Message'
                }
            }
        }.start()

        PooledActors.actor {
            actor << 'Message31'
            replyLatch.await()
            actor << 'Enabled message'
            react {a, b->
                latch.countDown()
            }
        }.start()

        latch.await()
        assertNotNull exception1
        assert exception1 instanceof IllegalStateException
        assertNotNull exception2
        assert exception2 instanceof MissingMethodException
    }
}
