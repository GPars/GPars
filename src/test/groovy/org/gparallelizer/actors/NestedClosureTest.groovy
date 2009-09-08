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
