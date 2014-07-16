package groovyx.gpars.dataflow.remote

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.remote.netty.NettyTransportProvider
import spock.lang.Specification
import spock.lang.Timeout

class RemoteDataflowsDataflowQueueWithServerTest extends Specification {
    def static HOST = "localhost"
    def static PORT = 9031

    def setupSpec() {
        NettyTransportProvider.startServer(HOST, PORT)
    }

    def cleanupSpec() {
        NettyTransportProvider.stopServer()
    }

    RemoteDataflowQueueFuture publishNewQueueAndGetRemotely(DataflowQueue queue, String queueName) {
        RemoteDataflows.publish queue, queueName
        RemoteDataflows.getDataflowQueue HOST, PORT, queueName
    }

    @Timeout(5)
    def "can retrieve published DataflowQueue"() {
        setup:
        def queue = new DataflowQueue()
        def queueName = "test-queue-0"

        when:
        def remoteQueue = publishNewQueueAndGetRemotely(queue, queueName) get()

        then:
        remoteQueue != null
    }

    @Timeout(5)
    def "can get item from queue"() {
        setup:
        def queue = new DataflowQueue()
        def queueName = "test-queue-1"
        def testValue = "test-value"

        when:
        def remoteQueue = publishNewQueueAndGetRemotely(queue, queueName) get()

        queue << testValue
        def receivedTestValue = remoteQueue.val

        sleep 500
        NettyTransportProvider.stopClients()

        then:
        receivedTestValue == testValue
    }

    @Timeout(5)
    def "can put item to queue"() {
        setup:
        def queue = new DataflowQueue()
        def queueName = "test-queue-2"
        def testValue = "test-value"

        when:
        def remoteQueue = publishNewQueueAndGetRemotely(queue, queueName) get()

        remoteQueue << testValue
        def receivedTestValue = queue.val

        sleep 500
        NettyTransportProvider.stopClients()

        then:
        receivedTestValue == testValue
    }
}
