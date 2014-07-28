package groovyx.gpars.agent.remote

import groovyx.gpars.agent.Agent
import groovyx.gpars.remote.LocalHost
import groovyx.gpars.remote.netty.NettyTransportProvider
import spock.lang.Specification
import spock.lang.Timeout

class RemoteAgentsWithServerTest extends Specification {
    def static HOST = "localhost"
    def static PORT = 9677

    def setupSpec() {
        def serverLocalHost = new LocalHost()
        NettyTransportProvider.startServer HOST, PORT, serverLocalHost
    }

    def cleanupSpec() {
        NettyTransportProvider.stopServer()
    }

    @Timeout(5)
    def "can retrieve published Agent with remote closure execution policy"() {
        setup:
        Agent<String> agent = new Agent<>("test-agent")
        def agentName = "test-agent-1"

        when:
        RemoteAgents.publish agent, agentName

        def remoteAgent = RemoteAgents.get HOST, PORT, agentName, ClojureExecutionPolicy.REMOTE get()

        then:
        remoteAgent != null
    }
}
