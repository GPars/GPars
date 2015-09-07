// GPars - Groovy Parallel Systems
//
// Copyright © 2014  The original author or authors
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

package groovyx.gpars.integration.remote.actor

import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.remote.RemoteActors
import groovyx.gpars.integration.remote.RemoteSpecification
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Timeout

import java.util.concurrent.CountDownLatch

class RemoteActorTest extends RemoteSpecification {
    def static PORT = 9012

    @Shared
    RemoteActors serverRemoteActors

    @Shared
    RemoteActors clientRemoteActors

    def setupSpec() {
        serverRemoteActors = RemoteActors.create()
        serverRemoteActors.startServer getHostAddress(), PORT

        clientRemoteActors = RemoteActors.create()
    }

    def cleanupSpec() {
        serverRemoteActors.stopServer()
    }

    @Timeout(5)
    def "join RemoteActor"() {
        setup:
        def latch = new CountDownLatch(1)
        def testActor = Actors.actor {
            for (i in 1..3) {
                println i
                sleep 200
            }
            latch.countDown()
        }
        def actorName = "testActor-1"

        serverRemoteActors.publish testActor, actorName
        def remoteActor = clientRemoteActors.get getHostAddress(), PORT, actorName get()

        when:
        remoteActor.join()

        then:
        latch.await()
    }

    @Timeout(5)
    def "send message to RemoteActor"() {
        setup:
        def message = false
        def testActor = Actors.actor {
            react {
                message = it
            }
        }
        def actorName = "testActor-2"

        serverRemoteActors.publish testActor, actorName
        def remoteActor = clientRemoteActors.get getHostAddress(), PORT, actorName get()

        when:
        remoteActor << "test message"
        remoteActor.join()

        then:
        message == "test message"
    }

    @Timeout(5)
    def "get reply from RemoteActor"() {
        setup:
        def replyMessageLatch = new CountDownLatch(1)
        def replyMessage = false
        def testActor = Actors.actor {
            react {
                reply "test reply"
            }
        }
        def actorName = "testActor-3"

        serverRemoteActors.publish testActor, actorName
        def remoteActor = clientRemoteActors.get getHostAddress(), PORT, actorName get()

        when:
        Actors.actor {
            remoteActor << "test message"
            react {
                replyMessage = it
                replyMessageLatch.countDown()
            }
        }

        then:
        replyMessageLatch.await()
        replyMessage == "test reply"
    }

    @Timeout(5)
    def "test sendAndWait on RemoteActor"() {
        setup:
        def message = "message"
        def messageTest = "test"

        def testActor = Actors.reactor {
            message + it
        }
        def actorName = "testActor-4"

        serverRemoteActors.publish testActor, actorName
        def remoteActor = clientRemoteActors.get getHostAddress(), PORT, actorName get()

        when:
        def receivedMessage = remoteActor.sendAndWait(messageTest)

        then:
        receivedMessage.length() == message.length() + messageTest.length()
        receivedMessage.startsWith message
        receivedMessage.endsWith messageTest
    }
}
