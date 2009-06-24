package org.gparallelizer.dataflow

import java.util.concurrent.CountDownLatch
import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import java.util.concurrent.CyclicBarrier

public class DataFlowStreamTest extends GroovyTestCase {

    public void testStream() {
        final CountDownLatch latch = new CountDownLatch(1)

        final DataFlowStream stream = new DataFlowStream()
        final AbstractPooledActor thread = DataFlow.thread {
            stream << 10
            final DataFlowVariable variable = new DataFlowVariable()
            stream << variable
            latch.countDown()
            react {
                variable << 20
            }
        }

        latch.await()
        assertEquals 2, stream.length()
        assertEquals 10, stream.val
        assertEquals 1, stream.length()
        thread << 'Proceed'
        assertEquals 20, stream.val
        assertEquals 0, stream.length()
    }

    public void testTake() {
        final CountDownLatch latch = new CountDownLatch(1)

        final DataFlowStream stream = new DataFlowStream()
        final AbstractPooledActor thread = DataFlow.thread {
            final DataFlowVariable variable = new DataFlowVariable()
            stream << variable
            latch.countDown()
            react {
                variable << 20
            }
        }

        latch.await()
        assertEquals 1, stream.length()
        final DataFlowVariable retrievedVariable = stream.take()
        assert retrievedVariable instanceof DataFlowVariable
        assertEquals 0, stream.length()
        thread << 'Proceed'
        assertEquals 20, retrievedVariable.val
    }

    public void testIteration() {
        final CyclicBarrier barrier = new CyclicBarrier(2)

        final DataFlowStream stream = new DataFlowStream()
        final AbstractPooledActor thread = DataFlow.thread {
            (0..10).each {stream << it}
            barrier.await()
            react {
                stream << 11
                barrier.await()
            }
        }

        barrier.await()
        assertEquals 11, stream.length()
        stream.eachWithIndex {index, element -> assertEquals index, element }
        assertEquals 11, stream.length()

        thread << 'Proceed'
        barrier.await()
        assertEquals 12, stream.length()  //todo sometimes fails
        (0..10).each {
            assertEquals it, stream.val
        }
    }

}