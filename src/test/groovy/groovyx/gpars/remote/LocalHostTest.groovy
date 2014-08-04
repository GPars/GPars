package groovyx.gpars.remote

import spock.lang.Specification

class LocalHostTest extends Specification {

    def "test if start server can be executed only once"() {
        setup:
        def localHostMock = new LocalHostMock()

        when:
        localHostMock.startServer("localhost", 11223)
        localHostMock.startServer("localhost", 11224)

        then:
        thrown(IllegalStateException)
    }

    def "test if stop server cannot be executed if server is not started"() {
        setup:
        def localHostMock = new LocalHostMock()

        when:
        localHostMock.stopServer()

        then:
        thrown(IllegalStateException)
    }
}
