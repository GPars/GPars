package groovyx.gpars.dataflow.remote

import groovyx.gpars.dataflow.DataflowVariable
import spock.lang.Specification

class RemoteDataflowsTest extends Specification {

    def "retrieving not published DataflowVariable return null"() {
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
}
