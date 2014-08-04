package groovyx.gpars.dataflow.remote

import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.remote.netty.NettyTransportProvider
import spock.lang.Specification
import spock.lang.Timeout

class RemoteDataflowsDataflowVariableTest extends Specification {
    def static HOST = "localhost"
    def static PORT = 9020

    RemoteDataflows remoteDataflows

    void setup() {
        remoteDataflows = RemoteDataflows.create()
    }

    def "retrieving not published DataflowVariable returns null"() {
        when:
        def var = remoteDataflows.get DataflowVariable, "test-variable"

        then:
        var == null
    }

    def "can publish DataflowVariable"() {
        setup:
        DataflowVariable<String> var = new DataflowVariable<>()
        def varName = "test-variable"

        when:
        remoteDataflows.publish var, varName
        def publishedVar = remoteDataflows.get DataflowVariable, varName

        then:
        publishedVar == var
    }

    def "retrieving DataflowVariable from remote host returns Promise"() {
        setup:
        def varName = "test-variable"

        when:
        def varFuture = remoteDataflows.getVariable HOST, PORT, varName

        then:
        varFuture != null
        varFuture instanceof DataflowVariable
    }

    def "retrieving DataflowVariable from remote host returns the same Promise"() {
        setup:
        def varName = "test-variable"

        when:
        def varFuture1 = remoteDataflows.getVariable HOST, PORT, varName
        def varFuture2 = remoteDataflows.getVariable HOST, PORT, varName

        then:
        varFuture1.is(varFuture2)
    }

}
