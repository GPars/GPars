package groovyx.gpars.actor.remote

import groovyx.gpars.actor.Actors
import spock.lang.Specification

class RemoteActorTest extends Specification {
    def static HOSTNAME = 'localhost'
    def static PORT = 9000

    def "join RemoteActor"() {
        setup:
        def done = false
        def testActor = Actors.actor {
            for (i in 1..3) {
                println i
                sleep 200
            }
            done = true
        }
        RemoteActors.register(testActor, "testActor")
        def remoteActor = RemoteActors.get(HOSTNAME, PORT, "testActor").get()

        when:
        remoteActor.join()

        then:
        done
    }
}
