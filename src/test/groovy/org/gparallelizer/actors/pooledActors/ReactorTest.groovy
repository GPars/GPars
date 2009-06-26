package org.gparallelizer.actors.pooledActors

import java.util.concurrent.atomic.AtomicInteger

public class ReactorTest extends GroovyTestCase{

    public void testMessageProcessing() {
        final def group = new PooledActorGroup()
        final def result1 = new AtomicInteger(0)
        final def result2 = new AtomicInteger(0)
        final def result3 = new AtomicInteger(0)

        final def processor = group.reactor {
            2 * it
        }.start()

        final def a1 = group.actor {
            result1 = processor.sendAndWait(10)
        }
        a1.start()

        final def a2 = group.actor {
            result2 = processor.sendAndWait(20)
        }
        a2.start()

        final def a3 = group.actor {
            result3 = processor.sendAndWait(30)
        }
        a3.start()

        [a1, a2, a3]*.join()
        assertEquals 20, result1
        assertEquals 40, result2
        assertEquals 60, result3

        processor.stop()
        processor.join()
    }
}