package groovyx.gpars.dataflow.remote

import groovyx.gpars.dataflow.DataflowVariable
import spock.lang.Specification

class RemoteDataflowsTest extends Specification {

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
        def host = "dummy-host"
        def port = 12345 // dummy port
        def varName = "test-variable"

        when:
        def varFuture = RemoteDataflows.get host, port, varName

        then:
        varFuture != null
        varFuture instanceof RemoteDataflowVariableFuture
    }

    def "retrieving DataflowVariable from remote host returns Future based on the same inner variable"() {
        setup:
        def host = "dummy-host"
        def port = 12345 // dummy port
        def varName = "test-variable"

        when:
        def varFuture1 = RemoteDataflows.get host, port, varName
        def varFuture2 = RemoteDataflows.get host, port, varName

        then:
        varFuture1 != varFuture2
        varFuture1.remoteVariable != null
        varFuture1.remoteVariable == varFuture2.remoteVariable
    }
}
