// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2012  The original author or authors
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

import groovyx.gpars.actor.Actors
import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.group.NonDaemonPGroup
import groovyx.gpars.group.PGroupBuilder

import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author Vaclav Pech
 * Date: 13.4.2010
 */
public class AgentTest extends GroovyTestCase {
    public void testList() {
        def jugMembers = new Agent<List>(['Me'])  //add Me

        jugMembers.send { it.add 'James' }  //add James
        jugMembers.await()
        assert new HashSet(['Me', 'James']) == new HashSet(jugMembers.instantVal)

        final Thread t1 = Thread.start {
            jugMembers { it.add 'Joe' }  //add Joe
        }

        final Thread t2 = Thread.start {
            jugMembers { it.add 'Dave' }  //add Dave
            jugMembers << { it.add 'Alice' }  //add Alice
        }

        [t1, t2]*.join()
        assert new HashSet(['Me', 'James', 'Joe', 'Dave', 'Alice']) == new HashSet(jugMembers.val)
    }

    public void testCustomGroup() {
        final NonDaemonPGroup group = new NonDaemonPGroup(1)
        def jugMembers = group.agent(['Me'])  //add Me

        jugMembers.send { it.add 'James' }  //add James
        jugMembers.await()
        assert new HashSet(['Me', 'James']) == new HashSet(jugMembers.instantVal)

        group.shutdown()
        shouldFail RejectedExecutionException, {
            jugMembers.send 10
        }
    }

    public void testCustomThreadPool() {
        def jugMembers = new Agent<List>(['Me'])  //add Me
        final ThreadPoolExecutor pool = Executors.newFixedThreadPool(1)
        final def group = PGroupBuilder.createFromPool(pool)
        jugMembers.attachToThreadPool group.threadPool

        jugMembers.send { it.add 'James' }  //add James
        jugMembers.await()
        assert new HashSet(['Me', 'James']) == new HashSet(jugMembers.instantVal)

        pool.shutdown()
        pool.awaitTermination(30, TimeUnit.SECONDS)
        shouldFail RejectedExecutionException, {
            jugMembers.send 10
        }
        group.shutdown()
    }

    public void testFairAgent() {
        def jugMembers = new Agent<List>(['Me'])  //add Me
        jugMembers.makeFair()

        jugMembers { it.add 'James' }  //add James
        jugMembers.await()
        assert new HashSet(['Me', 'James']) == new HashSet(jugMembers.instantVal)
    }

    public void testAgentFactory() {
        def jugMembers = Agent.agent(['Me'])  //add Me
        jugMembers.makeFair()

        jugMembers.send { it.add 'James' }  //add James
        jugMembers.await()
        assert new HashSet(['Me', 'James']) == new HashSet(jugMembers.instantVal)
    }

    public void testFairAgentFactory() {
        def jugMembers = Agent.fairAgent(['Me'])  //add Me

        jugMembers.send { it.add 'James' }  //add James
        jugMembers.await()
        assert new HashSet(['Me', 'James']) == new HashSet(jugMembers.instantVal)
    }

    public void testListWithCloneCopyStrategy() {
        def jugMembers = new Agent<List>(['Me'], { it?.clone() })  //add Me

        jugMembers.send { updateValue it << 'James' }  //add James

        final Thread t1 = Thread.start {
            jugMembers.send { updateValue it << 'Joe' }  //add Joe
        }

        final Thread t2 = Thread.start {
            jugMembers << { updateValue it << 'Dave' }  //add Dave
            jugMembers << { updateValue it << 'Alice' }  //add Alice
        }

        [t1, t2]*.join()
        assert new HashSet(['Me', 'James', 'Joe', 'Dave', 'Alice']) == new HashSet(jugMembers.val)
    }

    public void testCounter() {
        final Agent counter = new Agent<Long>(0L)

        final Thread t1 = Thread.start {
            counter << { updateValue it + 1 }
        }

        final Thread t2 = Thread.start {
            counter << { updateValue it + 6 }
        }

        final Thread t3 = Thread.start {
            counter << { updateValue it - 2 }
        }

        [t1, t2, t3]*.join()

        assert 5 == counter.val
    }

    public void testAsyncVal() {
        final Agent counter = new Agent<Long>(0L)

        final Thread t1 = Thread.start {
            counter << { updateValue it + 1 }
        }

        final Thread t2 = Thread.start {
            counter << { updateValue it + 6 }
        }

        final Thread t3 = Thread.start {
            counter << { updateValue it - 2 }
        }

        [t1, t2, t3]*.join()

        final def result = new DataflowVariable()
        counter.valAsync {
            result << it
        }

        assert 5 == result.val
    }

    public void testExplicitReply() {
        final Agent counter = new Agent<Long>(0L)

        def result = new DataflowQueue()
        Actors.actor {
            counter << { owner.send 'Explicit reply'; owner.send 10 }
            react { a ->
                react { b ->
                    result << a
                    result << b
                }
            }
        }

        assert 'Explicit reply' == result.val
        assert 10 == result.val
    }

    public void testDirectMessage() {
        final Agent counter = new Agent<Long>(0L)

        counter << null
        assertNull counter.val

        counter << 10
        assert 10 == counter.val

        counter << 20
        assert 20 == counter.val

        counter << { updateValue(it + 10) }
        assert 30 == counter.val

    }

    public void testDirectMessageOnNullInitialValue() {
        final Agent counter = new Agent<Long>()

        counter << 10
        assert 10 == counter.val
    }

    public void testNullInitialValue() {
        final Agent counter = new Agent<Long>()

        final def result = new DataflowVariable()
        counter << { result << it }
        assertNull result.val
    }

    public void testReplies() {
        final Agent counter = new Agent<Long>(0L)

        def result = new DataflowVariable()
        Actors.actor {
            counter << { owner.send it }
            react {
                result << it
            }
        }
        assert 0 == result.val

        result = new DataflowVariable()
        Actors.actor {
            counter << { owner.send null }
            react {
                result << it
            }
        }
        assertNull result.val
    }

    public void testAwait() {
        final Agent counter = new Agent<Long>(0L)

        final AtomicBoolean flag = new AtomicBoolean(false)
        counter << {
            updateValue it + 1
            Thread.sleep 3000
            updateValue it + 1
            flag.set true
        }
        assertFalse flag.get()
        counter.await()
        assert flag.get()
    }

    public void testInstantVal() {
        final Agent counter = new Agent<Long>(0L)

        assert 0 == counter.instantVal
        counter << {
            updateValue it + 1
        }
        counter.await()
        assert 1 == counter.instantVal
        assert 1 == counter.val
    }

    public void testIncompatibleMessageType() {
        final Agent counter = new Agent<Long>(0L)
        counter << 'test'
        assert 'test' == counter.val
        counter << 1L
        assert 1L == counter.val
    }

    public void testNullMessage() {
        final Agent counter = new Agent<Long>(0L)
        counter << 'test'

        final DataflowVariable result = new DataflowVariable<Long>()

        counter.valAsync {
            result << it
        }
        assert 'test' == result.val
    }

    public void testErrors() {
        def jugMembers = new Agent<List>()
        assert jugMembers.errors.empty
        assert !jugMembers.hasErrors()

        jugMembers.send { throw new IllegalStateException('test1') }
        jugMembers.send { throw new IllegalArgumentException('test2') }
        jugMembers.await()

        assert jugMembers.hasErrors()
        List errors = jugMembers.errors
        assert 2 == errors.size()
        assert errors[0] instanceof IllegalStateException
        assert 'test1' == errors[0].message
        assert errors[1] instanceof IllegalArgumentException
        assert 'test2' == errors[1].message

        assert jugMembers.errors.empty
        assert !jugMembers.hasErrors()
    }

    public void testWaitWithinCommand() {
        final result = new DataflowVariable()
        def agent = new Agent(1)
        agent.sendAndWait {
            try {
                agent.sendAndWait { result << 20 }
                result.bind(10)
            } catch (all) {
                result.bind(all)
            }
        }
        assert result.val instanceof IllegalStateException
    }

    public void testValWithinCommand() {
        final result = new DataflowVariable()
        def agent = new Agent(1)
        agent.sendAndWait {
            try {
                println "Agent has value $agent.val"
                result.bind(10)
            } catch (all) {
                result.bind(all)
            }
        }
        assert result.val instanceof IllegalStateException
    }
}