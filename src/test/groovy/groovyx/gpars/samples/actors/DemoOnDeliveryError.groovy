//  GPars (formerly GParallelizer)
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

package groovyx.gpars.samples.actors

import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.impl.AbstractPooledActor
import java.util.concurrent.CyclicBarrier

/**
 * Shows possibilities to handle message delivery errors.
 * When an actor terminates, unprocessed messages from its queue have their onDeliveryError() method called.
 * The onDeliveryError() method can, for example, send a notification back to the original sender of the message.
 */

Actors.defaultPooledActorGroup.resize 10
final CyclicBarrier barrier = new CyclicBarrier(2)

final AbstractPooledActor actor = Actors.actor {
    barrier.await()
    react {
        stop()
    }
}

final AbstractPooledActor me
me = Actors.actor {
    def message1 = 1
    def message2 = 2
    def message3 = 3

    message2.metaClass.onDeliveryError = {->
        me << "Could not deliver $delegate"
    }

    message3.metaClass.onDeliveryError = {->
        me << "Could not deliver $delegate"
    }

    actor << message1
    actor << message2
    actor << message3
    Thread.sleep 1000
    barrier.await()

    react {a ->
        println a
        react {b ->
            println b
            System.exit 0
        }
    }

}

System.in.read()
