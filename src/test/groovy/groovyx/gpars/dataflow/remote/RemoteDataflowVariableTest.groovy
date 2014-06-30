package groovyx.gpars.dataflow.remote

import groovyx.gpars.remote.RemoteHost
import spock.lang.Specification

class RemoteDataflowVariableTest extends Specification {
    RemoteHost remoteHost

    def setup() {
        remoteHost = Mock()
    }

    def "whenBound sends message to RemoteHost"() {
        setup:
        RemoteDataflowVariable<String> variable = new RemoteDataflowVariable<>(remoteHost)

        when:
        variable << "bind test"

        then:
        variable.isBound()
    }
}
