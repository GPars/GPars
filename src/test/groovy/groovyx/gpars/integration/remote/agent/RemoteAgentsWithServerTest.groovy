package groovyx.gpars.integration.remote.agent

import groovyx.gpars.agent.Agent
import groovyx.gpars.agent.remote.AgentClosureExecutionPolicy
import groovyx.gpars.agent.remote.RemoteAgent
import groovyx.gpars.agent.remote.RemoteAgents
import groovyx.gpars.integration.remote.RemoteSpecification
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Timeout

import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier

class RemoteAgentsWithServerTest extends RemoteSpecification {
    def static PORT = 9677

    @Shared
    RemoteAgents serverRemoteAgents

    @Shared
    RemoteAgents clientRemoteAgents

    def setupSpec() {
        serverRemoteAgents = RemoteAgents.create()
        serverRemoteAgents.startServer getHostAddress(), PORT

        clientRemoteAgents = RemoteAgents.create()
    }

    def cleanupSpec() {
        serverRemoteAgents.stopServer()
    }

    RemoteAgent publishAndRetrieveRemoteAgent(Agent agent, String name, AgentClosureExecutionPolicy policy) {
        serverRemoteAgents.publish agent, name
        sleep 250
        def remoteAgent = clientRemoteAgents.get getHostAddress(), PORT, name get()
        remoteAgent.executionPolicy = policy
        return remoteAgent
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
    def "can update state of Agent with remote closure execution"() {
        setup:
        def agentState = "test-agent-state"
        Agent<String> agent = new Agent<>(agentState)
        def agentName = "test-agent-2"
        def updateState1 = "test-agent-state-update-1"
        def updateState2 = "test-agent-state-update-2"
        def barrier = new CyclicBarrier(2)
        agent.addListener { oldVal, newVal ->
            barrier.await()
        }

        when:
        def remoteAgent = publishAndRetrieveRemoteAgent agent, agentName, AgentClosureExecutionPolicy.REMOTE

        then:
        remoteAgent != null

        when:
        barrier.reset()
        remoteAgent << { updateValue updateState1 }

        then:
        barrier.await()
        agent.val == updateState1

        when:
        barrier.reset()
        remoteAgent << updateState2

        then:
        barrier.await()
        agent.val == updateState2
    }

    @Timeout(5)
    def "can update state of Agent with local closure execution"() {
        setup:
        def agentState = "test-agent-state"
        Agent<String> agent = new Agent<>(agentState)
        def agentName = "test-agent-3"
        def updateState = "test-agent-state-update"
        def latch = new CountDownLatch(1)
        agent.addListener { oldVal, newVal ->
            latch.countDown()
        }

        when:
        def remoteAgent = publishAndRetrieveRemoteAgent agent, agentName, AgentClosureExecutionPolicy.LOCAL

        then:
        remoteAgent != null

        when:
        remoteAgent << { updateValue updateState }

        then:
        latch.await()
        agent.val == updateState
    }
}
