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

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.DataflowVariable

/**
 * @author Vaclav Pech
 */
class DefaultActorCreationTest extends GroovyTestCase {
    public void testCreationWithAct() {
        final def result = new DataflowVariable()
        final def thread = new DataflowVariable()
        final def actor = [act: {
            result << 'Received'
            thread << Thread.currentThread()
        }] as DefaultActor
        actor.start()
        assert result.val == 'Received'
        assert thread.val != Thread.currentThread()
        actor.join()
        assert !actor.isActive()
    }

    public void testCreationWithoutAct() {
        final def actor = new DefaultActor()

        final def error = new DataflowVariable()
        final def stopped = new DataflowVariable()
        actor.metaClass.onException {
            error << it
        }
        actor.metaClass.afterStop {
            stopped << true
        }
        actor.start()
        assert error.val instanceof UnsupportedOperationException
        stopped.val
        assert !actor.isActive()

        shouldFail(IllegalStateException) {
            actor 'Message'
        }
        assert !actor.isActive()
    }

    public void testCreationWithClosure() {
        final def result = new DataflowVariable()
        final def thread = new DataflowVariable()
        final def actor = new DefaultActor({
            result << 'Received'
            thread << Thread.currentThread()
        })
        actor.start()
        assert result.val == 'Received'
        assert thread.val != Thread.currentThread()
        actor.join()
        assert !actor.isActive()
    }

    public void testMessagingWithAct() {
        final result = new DataflowVariable()
        def actor
        actor = [act: {
            actor.react {
                result << it
            }
        }] as DefaultActor
        actor.start()
        actor 'Message'
        assert result.val == 'Message'
        actor.join()
        assert !actor.isActive()
    }

    public void testMessagingWithClosure() {
        final result = new DataflowVariable()
        final actor = new DefaultActor({
            react {
                result << it
            }
        })
        actor.start()
        actor 'Message'
        assert result.val == 'Message'
        actor.join()
        assert !actor.isActive()
    }

    public void testNullMessagingWithAct() {
        final result = new DataflowVariable()
        def actor
        actor = [act: {
            actor.react {
                result << it
            }
        }] as DefaultActor
        actor.start()
        actor null
        assert result.val == null
        actor.join()
        assert !actor.isActive()
    }

    public void testNullMessagingWithClosure() {
        final def result = new DataflowVariable()
        final def actor = new DefaultActor({
            react {
                result << it
            }
        })
        actor.start()
        actor null
        assert result.val == null
        actor.join()
        assert !actor.isActive()
    }

    public void testLoopingWithAct() {
        final def result = new DataflowQueue()
        def actor
        actor = [act: {
            actor.loop {
                react {
                    result << it
                }
            }
        }] as DefaultActor
        actor.start()
        actor 'Message1'
        actor 'Message2'
        actor 'Message3'
        assert result.val == 'Message1'
        assert result.val == 'Message2'
        assert result.val == 'Message3'
        actor.stop()
        actor.join()
        assert !actor.isActive()
    }

    public void testLoopingWithClosure() {
        final def result = new DataflowQueue()
        final def actor = new DefaultActor({
            loop {
                react {
                    result << it
                }
            }
        })
        actor.start()
        actor 'Message1'
        actor 'Message2'
        actor 'Message3'
        assert result.val == 'Message1'
        assert result.val == 'Message2'
        assert result.val == 'Message3'
        actor.stop()
        actor.join()
        assert !actor.isActive()
    }

    public void testRepliesWithAct() {
        def actor
        actor = [act: {
            actor.react {
                reply it
                react {
                    sender << it
                }
            }
        }] as DefaultActor
        actor.start()
        assert 'Message1' == actor.sendAndWait('Message1')
        assert 'Message2' == actor.sendAndWait('Message2')
        actor.join()
        assert !actor.isActive()
    }

    public void testRepliesWithClosure() {
        final def actor = new DefaultActor({
            react {
                reply it
                react {
                    sender << it
                }
            }
        })
        actor.start()
        assert 'Message1' == actor.sendAndWait('Message1')
        assert 'Message2' == actor.sendAndWait('Message2')
        actor.join()
        assert !actor.isActive()
    }


    public void testContinuationStyleWithAct() {
        final def result = new DataflowVariable()
        final def continuationResult = new DataflowVariable()
        final actor = [act: {
            actor.react {
                result << it
            }
            continuationResult << 'Reached'
        }] as DefaultActor
        actor.start()
        actor 'Message'
        assert result.val == 'Message'
        actor.join()
        assert !actor.isActive()
        assert continuationResult.isBound()
    }

    public void testContinuationStyleWithClosure() {
        final def result = new DataflowVariable()
        final def continuationResult = new DataflowVariable()
        final actor = new DefaultActor({
            react {
                result << it
            }
            continuationResult << 'Reached'
        })
        actor.start()
        actor 'Message'
        assert result.val == 'Message'
        actor.join()
        assert !actor.isActive()
        assert continuationResult.isBound()
    }
}
