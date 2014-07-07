package groovyx.gpars.dataflow.remote

import groovyx.gpars.dataflow.DataflowBroadcast
import spock.lang.Specification
import spock.lang.Timeout

class RemoteDataflowsDataflowBroadcastTest extends Specification {

    def "retrieving ReadChannel of not published DataflowBroadcast returns null"() {
        when:
        def stream = RemoteDataflows.getReadChannel "test-broadcast"

        then:
        stream == null
    }

    @Timeout(5)
    def "can publish DataflowBroadcast"() {
        setup:
        DataflowBroadcast broadcastStream = new DataflowBroadcast()
        def broadcastName = "test-broadcast"
        def testMessage = "test message"

        when:
        RemoteDataflows.publish broadcastStream, broadcastName
        def publishedStream = RemoteDataflows.getReadChannel broadcastName

        broadcastStream << testMessage

        then:
        publishedStream.val == testMessage
    }
}
