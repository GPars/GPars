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

import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.remote.RemoteActors
import groovyx.gpars.integration.remote.RemoteSpecification
import spock.lang.Specification
import spock.lang.Timeout

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class RemoteActorsWithNamesTest extends RemoteSpecification {
    @Timeout(5)
    def "register and get Actor using its name"() {
        setup:
        def serverRemoteActors = RemoteActors.create "test-group"
        def clientRemoteActors = RemoteActors.create "test-group"
        serverRemoteActors.startServer getHostAddress(), 22123
        def actor = Actors.reactor { it -> null }
        serverRemoteActors.publish actor, "test-actor"

        when:
        def remoteActor = tryGetActorByUrl clientRemoteActors, "test-actor"

        then:
        remoteActor != null

        cleanup:
        serverRemoteActors.stopServer()
    }

    @Timeout(5)
    def "register and get Actor using its name and group"() {
        setup:
        def serverRemoteActors = RemoteActors.create "test-group-1"
        def clientRemoteActors = RemoteActors.create "test-group-2"
        serverRemoteActors.startServer getHostAddress(), 22124
        def actor = Actors.reactor { it -> null }
        serverRemoteActors.publish actor, "test-actor"

        when:
        def remoteActor = tryGetActorByUrl clientRemoteActors, "test-group-1/test-actor"

        then:
        remoteActor != null

        cleanup:
        serverRemoteActors.stopServer()
    }

    @Timeout(5)
    def "register and get Actor using its name and group and send message to it"() {
        setup:
        def serverRemoteActors = RemoteActors.create "test-group-1"
        def clientRemoteActors = RemoteActors.create "test-group-2"
        serverRemoteActors.startServer getHostAddress(), 22125

        def latch = new CountDownLatch(1)
        def actor = Actors.actor { latch.countDown() }
        serverRemoteActors.publish actor, "test-actor"

        when:
        def remoteActor = tryGetActorByUrl clientRemoteActors, "test-group-1/test-actor"
        remoteActor << "test"
        latch.await()

        then:
        "ok"

        cleanup:
        serverRemoteActors.stopServer()
    }

    Actor tryGetActorByUrl(RemoteActors remoteActors, String actorUrl) {
        while (true) {
            try {
                return remoteActors.get(actorUrl).get(10, TimeUnit.MILLISECONDS)
            } catch (TimeoutException e) {
                System.err << "Retry\n"
            }
        }
    }
}
