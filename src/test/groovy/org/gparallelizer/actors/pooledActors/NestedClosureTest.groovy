package org.gparallelizer.actors.pooledActors

import org.gparallelizer.dataflow.DataFlowVariable

public class NestedClosureTest extends GroovyTestCase{
    public void testNestedClosures() {
        final def result = new DataFlowVariable<Integer>()

        final def group = new PooledActorGroup(20)

        final PooledActor actor = group.actor {
            final def nestedActor = group.actor {
                react {
                    reply 20
                }
            }.start()
            result << nestedActor.sendAndWait(10)
        }
        actor.start()
        assertEquals 20, result.val
    }
}