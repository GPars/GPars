package groovyx.gpars.dataflow.remote

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.remote.LocalHost
import groovyx.gpars.remote.netty.NettyTransportProvider
import spock.lang.Specification
import spock.lang.Timeout

import java.util.concurrent.CountDownLatch

class RemoteDataflowQueueTest extends Specification {
    def static HOST = "localhost"
    def static PORT = 9201

    def setupSpec() {
        def serverLocalHost = new LocalHost()
        NettyTransportProvider.startServer HOST, PORT, serverLocalHost
    }

    def cleanupSpec() {
        NettyTransportProvider.stopServer()
    }

    RemoteDataflowQueue publishNewQueueAndGetRemotely(DataflowQueue queue, String queueName) {
        RemoteDataflows.publish queue, queueName
        RemoteDataflows.getDataflowQueue HOST, PORT, queueName get()
    }

    @Timeout(5)
    def "test if not bounded DataflowVariable can be pushed into DataflowQueue and become bounded later"() {
        setup:
        def queue = new DataflowQueue()
        def queueName = "test-queue-1"
        def remoteQueue = publishNewQueueAndGetRemotely queue, queueName
        def testVariable = new DataflowVariable()

        when:
        remoteQueue << testVariable

        def resultVariable = new DataflowVariable()
        queue.whenBound {
            resultVariable << it
        }

        testVariable << queueName


        then:
        resultVariable.val == queueName
    }
}
