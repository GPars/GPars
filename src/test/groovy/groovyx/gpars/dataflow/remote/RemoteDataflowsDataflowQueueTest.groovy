package groovyx.gpars.dataflow.remote

import groovyx.gpars.dataflow.DataflowQueue
import spock.lang.Specification

class RemoteDataflowsDataflowQueueTest extends Specification {
    def static HOST = "localhost"
    def static PORT = 9030

    def "retrieving not published DataflowQueue returns null"() {
        when:
        def queue = RemoteDataflows.getDataflowQueue "test-queue"

        then:
        queue == null
    }

    def "can publish DataflowQueue"() {
        setup:
        DataflowQueue<String> queue = new DataflowQueue<>()
        def queueName = "test-queue"

        when:
        RemoteDataflows.publish queue, queueName
        def publishedQueue = RemoteDataflows.getDataflowQueue queueName

        then:
        publishedQueue == queue
    }

    def "retrieving DataflowQueue from remote host returns Future"() {
        setup:
        def queueName = "test-queue"

        when:
        def queueFuture = RemoteDataflows.getDataflowQueue HOST, PORT, queueName

        then:
        queueFuture != null
        queueFuture instanceof RemoteDataflowQueueFuture
    }

    def "retrieving DataflowQueue from remote host returns Future based on the same inner variable"() {
        // TODO don't use private fields
        setup:
        def queueName = "test-queue"

        when:
        def queueFuture1 = RemoteDataflows.getDataflowQueue HOST, PORT, queueName
        def queueFuture2 = RemoteDataflows.getDataflowQueue HOST, PORT, queueName

        then:
        !queueFuture1.is(queueFuture2)
        queueFuture1.remoteQueue != null
        queueFuture1.remoteQueue.is(queueFuture2.remoteQueue)
    }
}
