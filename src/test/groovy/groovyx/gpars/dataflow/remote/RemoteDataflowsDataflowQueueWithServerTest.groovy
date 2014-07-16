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

    @Timeout(5)
    def "can retrieve published DataflowQueue"() {
        setup:
        def queueName = "test-queue-1"
        def queue = new DataflowQueue()
        def testValue = "test-value-1"

        when:
        RemoteDataflows.publish queue, queueName
        def remoteQueue = RemoteDataflows.get HOST, PORT, queueName get()

        queue << testValue
        def receivedTestValue = remoteQueue.val

        sleep 500
        NettyTransportProvider.stopClients()

        then:
        receivedTestValue == testValue
    }
}
