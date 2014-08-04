package groovyx.gpars.dataflow.remote

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.remote.LocalHost
import groovyx.gpars.remote.netty.NettyTransportProvider
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Timeout

class RemoteDataflowsDataflowQueueWithServerTest extends Specification {
    def static HOST = "localhost"
    def static PORT = 9031

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

    RemoteDataflowQueue publishNewQueueAndGetRemotely(DataflowQueue queue, String queueName) {
        serverRemoteDataflows.publish queue, queueName
        clientRemoteDataflows.getDataflowQueue HOST, PORT, queueName get()
    }

    @Timeout(5)
    def "can retrieve published DataflowQueue"() {
        setup:
        def queue = new DataflowQueue()
        def queueName = "test-queue-0"

        when:
        def remoteQueue = publishNewQueueAndGetRemotely(queue, queueName)

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
        def remoteQueue = publishNewQueueAndGetRemotely(queue, queueName)

        queue << testValue
        def receivedTestValue = remoteQueue.val

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
        def remoteQueue = publishNewQueueAndGetRemotely(queue, queueName)

        remoteQueue << testValue
        def receivedTestValue = queue.val

        then:
        receivedTestValue == testValue
    }
}
