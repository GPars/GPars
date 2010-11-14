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

package groovyx.gpars.benchmark.akka

import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.DefaultActor

class TimerStatefulActor extends DefaultActor {
    boolean timing = false
    long startTime = 0

    void act() {
        loop {
            react { message ->
                switch (message) {
                    case StartMessage.instance:
                        if (!timing) {
                            startTime = System.currentTimeMillis()
                            timing = true
                        }
                        break
                    case StopMessage.instance:
                        if (timing) {
                            def endTime = System.currentTimeMillis()
                            println endTime - startTime
                            timing = false
                        }
                        break
                    case CancelMessage.instance:
                        terminate()
                }
            }
        }
    }
}

class NodeActor extends DefaultActor {
    Actor timer
    Actor follower
    int nodeId

    def connect(follower) {
        this.follower = follower
    }

    void act() {
        loop {
            react { message ->
                switch (message) {
                    case StartMessage.instance:
                        timer << message
                        follower << new TokenMessage(nodeId, 0)
                        break
                    case StopMessage.instance:
                        follower << message
                        terminate()
                        break
                    case TokenMessage:
                        if (message.id == nodeId) {
                            def nextValue = message.value + 1
                            if (nextValue == 1000000) {
                                timer << StopMessage.instance
                                timer << CancelMessage.instance
                                follower << StopMessage.instance
                                terminate()
                            } else {
                                follower << new TokenMessage(message.id, nextValue)
                            }
                        } else {
                            follower << message
                        }
                }
            }
        }
    }
}
	