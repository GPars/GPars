package org.gparallelizer.actors.pooledActors.safevariable

import org.gparallelizer.actors.pooledActors.SafeVariable
import org.gparallelizer.actors.pooledActors.PooledActors
import org.gparallelizer.dataflow.DataFlowVariable
import org.gparallelizer.dataflow.DataFlowStream
import java.util.concurrent.atomic.AtomicBoolean

public class SafeVariableTest extends GroovyTestCase {
    public void testList() {
        def jugMembers = new SafeVariable<List>(['Me'])  //add Me

        jugMembers.send {it.add 'James'}  //add James

        final Thread t1 = Thread.start {
            jugMembers.send {it.add 'Joe'}  //add Joe
        }

        final Thread t2 = Thread.start {
            jugMembers << {it.add 'Dave'}  //add Dave
            jugMembers << {it.add 'Alice'}  //add Alice
        }

        [t1, t2]*.join()
        assertEquals(['Me', 'James', 'Joe', 'Dave', 'Alice'], jugMembers.val)
    }

    public void testCounter() {
        final SafeVariable counter = new SafeVariable<Long>(0L)

        final Thread t1 = Thread.start {
            counter << {updateValue it + 1}
        }

        final Thread t2 = Thread.start {
            counter << {updateValue it + 6}
        }

        final Thread t3 = Thread.start {
            counter << {updateValue it - 2}
        }

        [t1, t2, t3]*.join()

        assert 5 == counter.val
    }

    public void testAsyncVal() {
        final SafeVariable counter = new SafeVariable<Long>(0L)

        final Thread t1 = Thread.start {
            counter << {updateValue it + 1}
        }

        final Thread t2 = Thread.start {
            counter << {updateValue it + 6}
        }

        final Thread t3 = Thread.start {
            counter << {updateValue it - 2}
        }

        [t1, t2, t3]*.join()

        final def result = new DataFlowVariable()
        counter.valAsync {
            result << it
        }

        assertEquals 5, result.val
    }

    public void testExplicitReply() {
        final SafeVariable counter = new SafeVariable<Long>(0L)

        def result = new DataFlowStream()
        PooledActors.actor {
            counter << {reply 'Explicit reply'; 10}
            react {a, b ->
                result << a
                result << b
            }
        }.start()

        assertEquals 'Explicit reply', result.val
        assertEquals 10, result.val
    }

    public void testDirectMessage() {
        final SafeVariable counter = new SafeVariable<Long>(0L)

        counter << null
        assertNull counter.val

        counter << 10
        assertEquals 10, counter.val

        counter << 20
        assertEquals 20, counter.val

        counter << {updateValue(it + 10)}
        assertEquals 30, counter.val

    }

    public void testDirectMessageOnNullInitialValue() {
        final SafeVariable counter = new SafeVariable<Long>()

        counter << 10
        assertEquals 10, counter.val
    }

    public void testNullInitialValue() {
        final SafeVariable counter = new SafeVariable<Long>()

        final def result = new DataFlowVariable()
        counter << {result << it}
        assertNull result.val
    }

    public void testReplies() {
        final SafeVariable counter = new SafeVariable<Long>(0L)

        def result = new DataFlowVariable()
        PooledActors.actor {
            counter << {it}
            react {
                result << it
            }
        }.start()
        assertEquals 0, result.val

        result = new DataFlowVariable()
        PooledActors.actor {
            counter << {null}
            react {
                result << it
            }
        }.start()
        assertNull result.val

        result = new DataFlowVariable()
        PooledActors.actor {
            counter << {}
            react {
                result << it
            }
        }.start()
        assertNull result.val

    }

    public void testAwait() {
        final SafeVariable counter = new SafeVariable<Long>(0L)

        final AtomicBoolean flag = new AtomicBoolean(false)
        counter << {
            updateValue it + 1
            Thread.sleep 2000
            updateValue it + 1
            flag.set true
        }
        assertFalse flag.get()
        counter.await()
        assert flag.get()
    }
}