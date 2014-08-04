package groovyx.gpars.dataflow.remote

import spock.lang.Specification


class RemoteDataflowsTest extends Specification {
    def "test if start server can be executed only once"() {
        setup:
        def remoteDataflows = RemoteDataflows.create()

        when:
        remoteDataflows.startServer("localhost", 11223)
        remoteDataflows.startServer("localhost", 11224)

        then:
        thrown(IllegalStateException)
    }

    def "test if stop server cannot be executed if server is not started"() {
        setup:
        def remoteDataflows = RemoteDataflows.create()

        when:
        remoteDataflows.stopServer()

        then:
        thrown(IllegalStateException)
    }
}
