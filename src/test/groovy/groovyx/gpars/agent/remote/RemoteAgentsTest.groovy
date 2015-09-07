package groovyx.gpars.agent.remote

import groovyx.gpars.agent.Agent
import groovyx.gpars.dataflow.DataflowVariable
import spock.lang.Specification

class RemoteAgentsTest extends Specification {
    RemoteAgents remoteAgents = RemoteAgents.create()

    def "retrieving not published Agent returns null"() {
        when:
        def agent = remoteAgents.get Agent, "test-agent"

        then:
        agent == null
    }

    def "can publish an Agent"() {
        setup:
        Agent<?> agent = new Agent<String>("test-agent");
        def name = "test-agent"

        when:
        remoteAgents.publish agent, name
        def retrievedAgent = remoteAgents.get Agent, name

        then:
        retrievedAgent == agent
    }

    def "retrieving an Agent from remote host returns Future"() {
        setup:
        def PORT = 9555
        def name = "test-agent"

        when:
        def agentFuture = remoteAgents.get "some-host", PORT, name

        then:
        agentFuture != null
    }
}
