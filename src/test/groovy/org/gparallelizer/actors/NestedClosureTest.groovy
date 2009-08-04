package org.gparallelizer.actors

import org.gparallelizer.dataflow.DataFlowVariable

public class NestedClosureTest extends GroovyTestCase {
    public void testNestedClosures() {
        final def result = new DataFlowVariable<Integer>()

        final def group = new ThreadActorGroup(20)

        final Actor actor = group.actor {
            final def nestedActor = group.actor {
                receive {
                    reply 20
                    stop()
                }
            }.start()
            result << nestedActor.sendAndWait(10)
            stop()
        }
        actor.start()
        assertEquals 20, result.val
    }

    public void testNestedClosuresForOneShotActors() {
        final def result = new DataFlowVariable<Integer>()

        final def group = new ThreadActorGroup(20)

        final Actor actor = group.oneShotActor {
            final def nestedActor = group.oneShotActor {
                receive {
                    reply 20
                }
            }.start()
            result << nestedActor.sendAndWait(10)
        }
        actor.start()
        assertEquals 20, result.val
    }


}