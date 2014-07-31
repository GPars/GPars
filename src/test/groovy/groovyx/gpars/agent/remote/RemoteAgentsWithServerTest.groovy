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

    RemoteAgent publishAndRetrieveRemoteAgent(Agent agent, String name, AgentClosureExecutionPolicy policy) {
        RemoteAgents.publish agent, name
        RemoteAgents.get HOST, PORT, name, policy get()
    }

    @Timeout(5)
    def "can retrieve published Agent with remote closure execution policy and retrieve state"() {
        setup:
        def agentState = "test-agent-state"
        Agent<String> agent = new Agent<>(agentState)
        def agentName = "test-agent-1"

        when:
        def remoteAgent = publishAndRetrieveRemoteAgent agent, agentName, AgentClosureExecutionPolicy.REMOTE

        then:
        remoteAgent != null

        when:
        def remoteState = remoteAgent.val

        then:
        remoteState == agentState
    }

    @Timeout(5)
    def "can send update state of Agent"() {
        setup:
        def agentState = "test-agent-state"
        Agent<String> agent = new Agent<>(agentState)
        def agentName = "test-agent-2"
        def updateState1 = "test-agent-state-update-1"
        def updateState2 = "test-agent-state-update-2"

        when:
        def remoteAgent = publishAndRetrieveRemoteAgent agent, agentName, executionPolicy

        then:
        remoteAgent != null

        when:
        remoteAgent << { updateValue updateState1 }
        sleep 500

        then:
        agent.val == updateState1

        when:
        remoteAgent << updateState2
        sleep 500

        then:
        agent.val == updateState2

        where:
        executionPolicy << AgentClosureExecutionPolicy.values()
    }
}
