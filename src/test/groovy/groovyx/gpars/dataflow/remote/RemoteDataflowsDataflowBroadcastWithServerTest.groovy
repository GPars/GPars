package groovyx.gpars.dataflow.remote

import groovyx.gpars.dataflow.DataflowBroadcast
import groovyx.gpars.dataflow.DataflowReadChannel
import groovyx.gpars.dataflow.Dataflows
import groovyx.gpars.remote.LocalHost
import groovyx.gpars.remote.netty.NettyTransportProvider
import spock.lang.Specification
import spock.lang.Timeout

import javax.xml.crypto.Data

class RemoteDataflowsDataflowBroadcastWithServerTest extends Specification {
    def static HOST = "localhost"
    def static PORT = 9177

    def setupSpec() {
        def serverLocalHost = new LocalHost()
        NettyTransportProvider.startServer HOST, PORT, serverLocalHost
    }

    def cleanupSpec() {
        NettyTransportProvider.stopServer()
    }

    DataflowReadChannel publishNewBroadcastAndGetRemotely(DataflowBroadcast broadcast, String broadcastName) {
        RemoteDataflows.publish broadcast, broadcastName
        RemoteDataflows.getReadChannel HOST, PORT, broadcastName get()
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
