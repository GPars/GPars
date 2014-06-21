package groovyx.gpars.actor.remote

import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.DefaultActor
import junit.framework.Test
import spock.lang.Specification


class RemoteActorsTest extends Specification {
    def HOST = "localhost"
    def PORT = 9000

    def "Register and get actor"() {
        setup:
        def testActor = new TestActor()
        def testMessage = "testMessage"

        when:
        testActor.start()
        RemoteActors.register(testActor, "testActor")
        def remoteActor = RemoteActors.get(HOST, PORT, "testActor").get()

        remoteActor << testMessage

        then:
        remoteActor != null;
        testActor.lastMessage == testMessage

        testActor.stop()
    }

    def "Register and get two actors"() {
        setup:
        def testActor1 = new TestActor()
        def testActor2 = new TestActor()
        def testMessage = "testMessage"

        when:
        testActor1.start()
        testActor2.start()
        RemoteActors.register(testActor1, "testActor1")
        RemoteActors.register(testActor2, "testActor2")

        def remoteActor1Future = RemoteActors.get(HOST, PORT, "testActor1")
        def remoteActor2Future = RemoteActors.get(HOST, PORT, "testActor2")

        def remoteActor1 = remoteActor1Future.get()
        def remoteActor2 = remoteActor2Future.get()

        remoteActor1 << testMessage + "1"
        remoteActor2 << testMessage + "2"

        then:
        remoteActor1 != null
        remoteActor2 != null
        testActor1.lastMessage == testMessage + "1"
        testActor2.lastMessage == testMessage + "2"

        testActor1.stop()
        testActor2.stop()
    }

    class TestActor extends DefaultActor {
        def lastMessage;

        @Override
        protected void act() {
            loop {
                react {
                    lastMessage = it
                }
            }
        }
    }
}
