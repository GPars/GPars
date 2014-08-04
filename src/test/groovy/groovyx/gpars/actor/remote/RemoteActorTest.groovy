package groovyx.gpars.actor.remote

import groovyx.gpars.actor.Actors
import groovyx.gpars.remote.netty.NettyTransportProvider
import spock.lang.Specification
import spock.lang.Timeout

import java.util.concurrent.CountDownLatch

class RemoteActorTest extends Specification {
    def static HOST = "localhost"
    def static PORT = 9012

    def setupSpec() {
        NettyTransportProvider.startServer(HOST, PORT)
    }

    def cleanupSpec() {
        NettyTransportProvider.stopServer()
    }

    @Timeout(5)
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
        def remoteActor = RemoteActors.get(HOST, PORT, "testActor").get()

        when:
        remoteActor.join()

        then:
        done
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

        RemoteActors.register(testActor, "testActor")
        def remoteActor = RemoteActors.get(HOST, PORT, "testActor").get()

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

        RemoteActors.register(testActor, "testActor")
        def remoteActor = RemoteActors.get(HOST, PORT, "testActor").get()

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
}
