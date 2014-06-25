package groovyx.gpars.actor.remote

import groovyx.gpars.actor.DefaultActor
import groovyx.gpars.remote.netty.NettyTransportProvider
import spock.lang.Specification
import spock.lang.Timeout

import java.util.concurrent.CountDownLatch


class RemoteActorsTest extends Specification {
    def static HOST = "localhost"
    def static PORT = 9011

    def setupSpec() {
        NettyTransportProvider.startServer(HOST, PORT)
    }

    def cleanupSpec() {
        NettyTransportProvider.stopServer()
    }

    @Timeout(5)
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
        testActor1.lastMessageLatch.await()
        testActor1.lastMessage == testMessage + "1"
        testActor2.lastMessageLatch.await()
        testActor2.lastMessage == testMessage + "2"

        testActor1.stop()
        testActor2.stop()
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
