package groovyx.gpars.dataflow.remote

import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.remote.netty.NettyTransportProvider
import spock.lang.Specification
import spock.lang.Timeout


class RemoteDataflowsDataflowVariableWithServerTest extends Specification {
    def static HOST = "localhost"
    def static PORT = 9021

    def setupSpec() {
        NettyTransportProvider.startServer HOST, PORT
    }

    def cleanupSpec() {
        NettyTransportProvider.stopServer()
    }

    @Timeout(5)
    def "can retrieve published DataflowVariable"() {
        setup:
        def variableName = "test-variable"
        def variable = new DataflowVariable<String>()
        def testValue = "test DataflowVariable"

        when:
        RemoteDataflows.publish variable, variableName
        def remoteVariable = RemoteDataflows.get HOST, PORT, variableName get()

        variable << testValue

        sleep 1000
        NettyTransportProvider.stopClients()

        then:
        remoteVariable.val == testValue
    }

    @Timeout(5)
    def "can retrieve published bound DataflowVariable"() {
        setup:
        def variableName = "test-variable"
        def variable = new DataflowVariable<String>()
        def testValue = "test DataflowVariable"

        when:
        variable << testValue

        RemoteDataflows.publish variable, variableName
        def remoteVariable = RemoteDataflows.get HOST, PORT, variableName get()

        sleep 1000
        NettyTransportProvider.stopClients()

        then:
        remoteVariable.val == testValue
    }

    @Timeout(5)
    def "can retrieve published DataflowVariable and bind it remotely"() {
        setup:
        def variableName = "test-variable"
        def variable = new DataflowVariable<String>()
        def testValue = "test DataflowVariable"

        when:
        RemoteDataflows.publish variable, variableName
        def remoteVariable = RemoteDataflows.get HOST, PORT, variableName get()

        remoteVariable << testValue

        sleep 1000
        NettyTransportProvider.stopClients()

        then:
        variable.val == testValue
    }
}
