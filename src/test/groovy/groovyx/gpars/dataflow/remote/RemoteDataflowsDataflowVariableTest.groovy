package groovyx.gpars.dataflow.remote

import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.remote.netty.NettyTransportProvider
import spock.lang.Specification
import spock.lang.Timeout

class RemoteDataflowsDataflowVariableTest extends Specification {
    def static HOST = "localhost"
    def static PORT = 9020

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
        def publishedVar = RemoteDataflows.get varName

        then:
        publishedVar == var
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

}
