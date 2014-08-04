package groovyx.gpars.dataflow.remote

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.DataflowVariable
import spock.lang.Specification

class RemoteDataflowsDataflowQueueTest extends Specification {
    RemoteDataflows remoteDataflows

    void setup() {
        remoteDataflows = RemoteDataflows.create()
    }

    def "retrieving not published DataflowQueue returns null"() {
        when:
        def queue = remoteDataflows.get DataflowQueue, "test-queue"

        then:
        queue == null
    }

    def "can publish DataflowQueue"() {
        setup:
        DataflowQueue<String> queue = new DataflowQueue<>()
        def queueName = "test-queue"

        when:
        remoteDataflows.publish queue, queueName
        def publishedQueue = remoteDataflows.get DataflowQueue, queueName

        then:
        publishedQueue == queue
    }

    def "retrieving DataflowQueue from remote host returns Future"() {
        setup:
        def HOST = "localhost"
        def PORT = 9030
        def queueName = "test-queue"

        when:
        def queuePromise = remoteDataflows.getDataflowQueue HOST, PORT, queueName

        then:
        queuePromise != null
        queuePromise instanceof DataflowVariable
    }

    def "retrieving DataflowQueue from remote host returns the same Promise"() {
        setup:
        def HOST = "localhost"
        def PORT = 9030
        def queueName = "test-queue"

        when:
        def queuePromise1 = remoteDataflows.getDataflowQueue HOST, PORT, queueName
        def queuePromise2 = remoteDataflows.getDataflowQueue HOST, PORT, queueName

        then:
        queuePromise1.is(queuePromise2)
    }
}
