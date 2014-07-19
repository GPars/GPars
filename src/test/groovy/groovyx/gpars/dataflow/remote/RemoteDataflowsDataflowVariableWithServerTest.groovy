package groovyx.gpars.dataflow.remote

import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.remote.LocalHost
import groovyx.gpars.remote.netty.NettyTransportProvider
import spock.lang.Specification
import spock.lang.Timeout


class RemoteDataflowsDataflowVariableWithServerTest extends Specification {
    def static HOST = "localhost"
    def static PORT = 9021

    def setupSpec() {
        LocalHost host = new LocalHost()
        NettyTransportProvider.startServer HOST, PORT, host
    }

    def cleanupSpec() {
        NettyTransportProvider.stopServer()
    }

    @Timeout(5)
    def "can retrieve published DataflowVariable"() {
        setup:
        def variableName = "test-variable-1"
        def variable = new DataflowVariable<String>()
        def testValue = "test DataflowVariable"

        when:
        RemoteDataflows.publish variable, variableName
        def remoteVariable = RemoteDataflows.get HOST, PORT, variableName get()

        variable << testValue

        then:
        remoteVariable.val == testValue
    }

    /*@Timeout(5)
    def "can retrieve published bound DataflowVariable"() {
        setup:
        def variableName = "test-variable-2"
        def variable = new DataflowVariable<String>()
        def testValue = "test DataflowVariable"

        when:
        variable << testValue

        RemoteDataflows.publish variable, variableName
        def remoteVariable = RemoteDataflows.get HOST, PORT, variableName get()

        then:
        remoteVariable.val == testValue
    }*/

    @Timeout(5)
    def "can retrieve published DataflowVariable and bind it remotely"() {
        setup:
        def variableName = "test-variable-3"
        def variable = new DataflowVariable<String>()
        def testValue = "test DataflowVariable"

        when:
        RemoteDataflows.publish variable, variableName
        def remoteVariable = RemoteDataflows.get HOST, PORT, variableName get()

        remoteVariable << testValue

        then:
        variable.val == testValue
    }
}
