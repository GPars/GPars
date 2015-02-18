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

import groovyx.gpars.actor.DefaultActor
import groovyx.gpars.actor.remote.RemoteActors
import groovyx.gpars.integration.remote.RemoteSpecification
import spock.lang.Shared
import spock.lang.Timeout

import java.util.concurrent.CountDownLatch


class RemoteActorsTest extends RemoteSpecification {
    def static PORT = 9011

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
    def "Register and get actor"() {
        setup:
        def testActor = new TestActor()
        def testMessage = "testMessage"

        when:
        testActor.start()
        serverRemoteActors.publish testActor, "testActor"
        def remoteActor = clientRemoteActors.get getHostAddress(), PORT, "testActor" get()
        remoteActor << testMessage

        then:
        remoteActor != null;
        testActor.lastMessageLatch.await()
        testActor.lastMessage == testMessage

        testActor.stop()
    }

    @Timeout(5)
    def "Register and get two actors"() {
        setup:
        def testActor1 = new TestActor()
        def testActor2 = new TestActor()
        def testMessage = "testMessage"

        when:
        testActor1.start()
        testActor2.start()
        serverRemoteActors.publish testActor1, "testActor1"
        serverRemoteActors.publish testActor2, "testActor2"

        def remoteActor1Promise = clientRemoteActors.get getHostAddress(), PORT, "testActor1"
        def remoteActor2Promise = clientRemoteActors.get getHostAddress(), PORT, "testActor2"

        def remoteActor1 = remoteActor1Promise.get()
        def remoteActor2 = remoteActor2Promise.get()

        remoteActor1 << testMessage + "1"
        remoteActor2 << testMessage + "2"

        then:
        remoteActor1 != null
        remoteActor2 != null
        testActor1.lastMessageLatch.await()
        testActor1.lastMessage == testMessage + "1"
        testActor2.lastMessageLatch.await()
        testActor2.lastMessage == testMessage + "2"

        testActor1.stop()
        testActor2.stop()
    }

    def "Cannot register actor under name with slashes"() {
        setup:
        def testActor = new TestActor()

        when:
        serverRemoteActors.publish testActor, "some-forbidden/name"

        then:
        thrown(IllegalArgumentException)
    }

    class TestActor extends DefaultActor {
        def lastMessage
        def lastMessageLatch

        TestActor() {
            lastMessageLatch = new CountDownLatch(1)
        }

        @Override
        protected void act() {
            loop {
                react {
                    println "message"
                    lastMessage = it
                    lastMessageLatch.countDown()
                }
            }
        }
    }


}
