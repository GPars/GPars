package groovyx.gpars.agent.remote

import groovyx.gpars.agent.Agent
import spock.lang.Specification

class RemoteAgentsTest extends Specification {
    def static HOST = "localhost"
    def static PORT = 9555

    def "retrieving not published Agent returns null"() {
        when:
        def agent = RemoteAgents.get "test-agent"

        then:
        agent == null
    }

    def "can publish an Agent"() {
        setup:
        Agent<?> agent = new Agent<String>("test-agent");
        def name = "test-agent"

        when:
        RemoteAgents.publish agent, name
        def retrievedAgent = RemoteAgents.get name

        then:
        retrievedAgent == agent
    }

    def "retrieving an Agent from remote host returns Future"() {
        setup:
        def name = "test-agent"

        when:
        def agentFuture = RemoteAgents.get HOST, PORT, name, policy

        then:
        agentFuture != null
        agentFuture instanceof RemoteAgentFuture

        where:
        policy << ClojureExecutionPolicy.values()
    }
}
