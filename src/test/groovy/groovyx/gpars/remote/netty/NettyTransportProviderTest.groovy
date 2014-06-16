package groovyx.gpars.remote.netty

import groovyx.gpars.actor.Actors
import spock.lang.Specification

class NettyTransportProviderTest extends Specification {
    def "Register"() {
        setup:
        def actor = Actors.actor {
            println "actor"
        }

        when:
        NettyTransportProvider.register(actor, "actor")

        then:
        NettyTransportProvider.localHost.localActors.size() == 1
    }
}
