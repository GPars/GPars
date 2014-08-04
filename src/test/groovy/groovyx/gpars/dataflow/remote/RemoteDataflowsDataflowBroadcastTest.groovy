package groovyx.gpars.dataflow.remote

import groovyx.gpars.dataflow.DataflowBroadcast
import groovyx.gpars.dataflow.DataflowVariable
import spock.lang.Specification
import spock.lang.Timeout

class RemoteDataflowsDataflowBroadcastTest extends Specification {
    RemoteDataflows remoteDataflows

    void setup() {
        remoteDataflows = RemoteDataflows.create()
    }

    def "retrieving not published DataflowBroadcast returns null"() {
        when:
        def stream = remoteDataflows.get DataflowBroadcast, "test-broadcast"

        then:
        stream == null
    }

    def "can publish DataflowBroadcast"() {
        setup:
        DataflowBroadcast broadcastStream = new DataflowBroadcast()
        def broadcastName = "test-broadcast"

        when:
        remoteDataflows.publish broadcastStream, broadcastName
        def publishedStream = remoteDataflows.get DataflowBroadcast, broadcastName

        then:
        publishedStream == broadcastStream
    }

    def "retrieving ReadChannel from remote host returns future"() {
        setup:
        def HOST = "dummy-host"
        def PORT = 9077 // dummy port
        def broadcastName = "test-broadcast"

        when:
        def streamFuture = remoteDataflows.getReadChannel HOST, PORT, broadcastName

        then:
        streamFuture != null
        streamFuture instanceof DataflowVariable
    }
}
