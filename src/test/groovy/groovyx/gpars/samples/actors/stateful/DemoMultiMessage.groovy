// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
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

package groovyx.gpars.samples.actors.stateful

import groovyx.gpars.actor.Actor
import groovyx.gpars.group.DefaultPGroup
import static java.util.concurrent.TimeUnit.SECONDS

/**
 * Demonstrates the ability to receive multiple messages and selectively reply to some of them.
 * Notice the ability to sent HashMaps as well as instances of the Offer class as messages.
 * A custom pooled actor group is used to group the actors with a single thread pool.
 * The actors, which have submitted their offers, terminate, if they don't hear back from the actor within a timeout.
 * The main threads joins all actors to wait for their termination, since we're using a non-daemon actor group.
 * @author Vaclav Pech
 */

final DefaultPGroup group = new DefaultPGroup(10)
class Messages {
    static def REPLY_TO_EARLY_BIRDS = 'Received your kind offer. Still collecting offers. If you know someone, who might be interested, please tell him.'
    static def REPLY = 'Received your kind offer. Now processing it and comparing with others.'
}

final Actor actor = group.actor {
    def messages = [:]
    react {offerA ->
        messages[offerA] = sender
        reply Messages.REPLY_TO_EARLY_BIRDS
        react {offerB ->
            messages[offerB] = sender
            reply Messages.REPLY_TO_EARLY_BIRDS
            react {offerC ->
                messages[offerC] = sender
                messages.values()*.send Messages.REPLY  //sent to all senders
                def winnerOffer = [offerA, offerB, offerC].min {it.price}
                messages[winnerOffer].send 'I accept your reasonable offer'  //sent to the winner only
                messages.findAll {it.key != winnerOffer}*.value*.send 'Maybe next time'  //sent to the losers only
            }
        }
    }
}

final def a1 = group.actor {
    actor new Offer(price: 10)
    loop {
        react(3, SECONDS) {
            println "Agent 1: $it"
        }
    }
}

final def a2 = group.actor {
    actor([price: 20])
    loop {
        react(3, SECONDS) {
            println "Agent 2: $it"
        }
    }
}

final def a3 = group.actor {
    actor new Offer(price: 5)
    loop {
        react(3, SECONDS) {
            println "Agent 3: $it"
        }
    }
}

actor.join()
[a1, a2, a3]*.stop()
[a1, a2, a3]*.join()

class Offer {
    int price
}


