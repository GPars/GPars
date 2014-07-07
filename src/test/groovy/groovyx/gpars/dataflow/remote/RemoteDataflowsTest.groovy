package groovyx.gpars.dataflow.remote

import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.remote.netty.NettyTransportProvider
import spock.lang.Specification
import spock.lang.Timeout

class RemoteDataflowsTest extends Specification {
    def static HOST = "localhost"
    def static PORT = 9021

    def "retrieving not published DataflowVariable returns null"() {
        when:
        def var = RemoteDataflows.get "test-variable"

        then:
        var == null
    }

    def "can publish DataflowVariable"() {
        setup:
        DataflowVariable<String> var = new DataflowVariable<>()
        def varName = "test-variable"

        when:
        RemoteDataflows.publish var, varName

        then:
        RemoteDataflows.get varName
    }

    def "retrieving DataflowVariable from remote host returns Future"() {
        setup:
        def varName = "test-variable"

        when:
        def varFuture = RemoteDataflows.get HOST, PORT, varName

        then:
        varFuture != null
        varFuture instanceof RemoteDataflowVariableFuture
    }

    def "retrieving DataflowVariable from remote host returns Future based on the same inner variable"() {
        setup:
        def varName = "test-variable"

        when:
        def varFuture1 = RemoteDataflows.get HOST, PORT, varName
        def varFuture2 = RemoteDataflows.get HOST, PORT, varName

        then:
        varFuture1 != varFuture2
        varFuture1.remoteVariable != null
        varFuture1.remoteVariable == varFuture2.remoteVariable
    }

    @Timeout(5)
    def "can retrieve published DataflowVariable"() {
        setup:
        def variableName = "test-variable"
        def variable = new DataflowVariable<String>()
        def testValue = "test DataflowVariable"

        when:
        NettyTransportProvider.startServer HOST, PORT
        RemoteDataflows.publish variable, variableName
        def remoteVariable = RemoteDataflows.get HOST, PORT, variableName get()

        variable << testValue

        then:
        remoteVariable.val == testValue
    }
}
