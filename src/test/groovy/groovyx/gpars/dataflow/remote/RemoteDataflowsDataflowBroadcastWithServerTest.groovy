package groovyx.gpars.dataflow.remote

import groovyx.gpars.dataflow.DataflowBroadcast
import groovyx.gpars.dataflow.DataflowReadChannel
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Timeout

class RemoteDataflowsDataflowBroadcastWithServerTest extends Specification {
    def static HOST = "localhost"
    def static PORT = 9177

    @Shared
    RemoteDataflows serverRemoteDataflows

    @Shared
    RemoteDataflows clientRemoteDataflows

    def setupSpec() {
        serverRemoteDataflows = RemoteDataflows.create()
        serverRemoteDataflows.startServer HOST, PORT

        clientRemoteDataflows = RemoteDataflows.create()
    }

    def cleanupSpec() {
        serverRemoteDataflows.stopServer()
    }

    DataflowReadChannel publishNewBroadcastAndGetRemotely(DataflowBroadcast broadcast, String broadcastName) {
        serverRemoteDataflows.publish broadcast, broadcastName
        clientRemoteDataflows.getReadChannel HOST, PORT, broadcastName get()
    }

    @Timeout(5)
    def "can retrieve published DataflowBroadcast"() {
        setup:
        DataflowBroadcast broadcast = new DataflowBroadcast()
        def broadcastName = "test-broadcast-0"

        when:
        def remoteChannel = publishNewBroadcastAndGetRemotely broadcast, broadcastName

        then:
        remoteChannel != null
    }

    @Timeout(5)
    def "can get item from DataflowBroadcast"() {
        setup:
        DataflowBroadcast broadcast = new DataflowBroadcast()
        def broadcastName = "test-broadcast-1"
        def testValue = "test-value"

        when:
        def remoteChannel = publishNewBroadcastAndGetRemotely broadcast, broadcastName
        broadcast << testValue

        def receivedTestValue = remoteChannel.val

        then:
        receivedTestValue == testValue
    }

    @Timeout(5)
    def "can get the same item from DataflowBroadcast with local and remote read channels"() {
        setup:
        DataflowBroadcast broadcast = new DataflowBroadcast()
        def broadcastName = "test-broadcast-2"
        def testValue = "test-value"

        when:
        def remoteChannel1 = publishNewBroadcastAndGetRemotely broadcast, broadcastName
        def remoteChannel2 = publishNewBroadcastAndGetRemotely broadcast, broadcastName
        def localChannel = broadcast.createReadChannel()

        broadcast << testValue

        then:
        remoteChannel1 != remoteChannel2
        [remoteChannel1, remoteChannel2, localChannel].collect { it.val } every { it == testValue }
    }
}
