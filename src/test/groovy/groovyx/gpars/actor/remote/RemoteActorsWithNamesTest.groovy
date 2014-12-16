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

package groovyx.gpars.actor.remote

import groovyx.gpars.actor.Actors
import org.apache.tools.ant.taskdefs.optional.extension.Specification
import spock.lang.Specification
import spock.lang.Timeout

import java.util.concurrent.CountDownLatch

class RemoteActorsWithNamesTest extends Specification {
    // @Timeout(5)
    def "register and get Actor using its name"() {
        setup:
        def serverRemoteActors = RemoteActors.create "test-group"
        def clientRemoteActors = RemoteActors.create "test-group"
        serverRemoteActors.startServer "192.168.0.2", 9123
        def actor = Actors.reactor { it -> null }
        serverRemoteActors.publish actor, "test-actor"

        when:
        def remoteActor = clientRemoteActors.get "test-actor" get()

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
        serverRemoteActors.startServer "192.168.0.2", 9124
        def actor = Actors.reactor { it -> null }
        serverRemoteActors.publish actor, "test-actor"

        when:
        def remoteActor = clientRemoteActors.get "test-group-1/test-actor" get()

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
        serverRemoteActors.startServer "192.168.0.2", 9125

        def latch = new CountDownLatch(1)
        def actor = Actors.actor { latch.countDown() }
        serverRemoteActors.publish actor, "test-actor"

        when:
        def remoteActor = clientRemoteActors.get "test-group-1/test-actor" get()
        remoteActor << "test"
        latch.await()

        then:
        "ok"

        cleanup:
        serverRemoteActors.stopServer()
    }
}
