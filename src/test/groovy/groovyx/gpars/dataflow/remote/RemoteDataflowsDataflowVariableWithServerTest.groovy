package groovyx.gpars.dataflow.remote

import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.remote.LocalHost
import groovyx.gpars.remote.netty.NettyTransportProvider
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Timeout


class RemoteDataflowsDataflowVariableWithServerTest extends Specification {
    def static HOST = "localhost"
    def static PORT = 9021

    @Shared
    RemoteDataflows remoteDataflows

    def setupSpec() {
        remoteDataflows = RemoteDataflows.create()
        remoteDataflows.startServer HOST, PORT
    }

    def cleanupSpec() {
        remoteDataflows.stopServer()
    }

    @Timeout(5)
    def "can retrieve published DataflowVariable"() {
        setup:
        def variableName = "test-variable-1"
        def variable = new DataflowVariable<String>()
        def testValue = "test DataflowVariable"

        when:
        remoteDataflows.publish variable, variableName
        def remoteVariable = remoteDataflows.getVariable HOST, PORT, variableName get()

        variable << testValue

        then:
        remoteVariable.val == testValue
    }

    @Timeout(5)
    def "can retrieve published bound DataflowVariable"() {
        setup:
        def variable = new DataflowVariable()

        when:
        variable << testValue

        remoteDataflows.publish variable, variableName
        def remoteVariable = remoteDataflows.getVariable HOST, PORT, variableName get()

        then:
        remoteVariable.val == testValue

        where:
        variableName << ["test-variable-2a", "test-variable-2b"]
        testValue << ["test DataflowVariable", null]
    }

    @Timeout(5)
    def "can retrieve published DataflowVariable and bind it remotely"() {
        setup:
        def variable = new DataflowVariable()

        when:
        remoteDataflows.publish variable, variableName
        def remoteVariable = remoteDataflows.getVariable HOST, PORT, variableName get()

        remoteVariable << testValue

        then:
        variable.val == testValue

        where:
        variableName << ["test-variable-3a", "test-variable-3b"]
        testValue << ["test DataflowVariable", null]
    }
}
