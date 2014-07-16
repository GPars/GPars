package groovyx.gpars.dataflow.remote

import groovyx.gpars.dataflow.DataflowBroadcast
import spock.lang.Specification
import spock.lang.Timeout

class RemoteDataflowsDataflowBroadcastTest extends Specification {

    def "retrieving ReadChannel of not published DataflowBroadcast returns null"() {
        when:
        def stream = RemoteDataflows.getBroadcastStream "test-broadcast"

        then:
        stream == null
    }

    def "can publish DataflowBroadcast"() {
        setup:
        DataflowBroadcast broadcastStream = new DataflowBroadcast()
        def broadcastName = "test-broadcast"

        when:
        RemoteDataflows.publish broadcastStream, broadcastName
        def publishedStream = RemoteDataflows.getBroadcastStream broadcastName

        then:
        publishedStream == broadcastStream
    }

    def "retrieving ReadChannel from remote host returns future"() {
        setup:
        def HOST = "dummy-host"
        def PORT = 9077 // dummy port
        def broadcastName = "test-broadcast"

        when:
        def streamFuture = RemoteDataflows.getReadChannel HOST, PORT, broadcastName

        then:
        streamFuture != null
        streamFuture instanceof RemoteDataflowReadChannelFuture
    }
}
