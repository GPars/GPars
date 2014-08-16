package groovyx.gpars.samples.remote.agent

import groovyx.gpars.agent.remote.AgentClosureExecutionPolicy
import groovyx.gpars.agent.remote.RemoteAgents

println "Remote Agent - remote"

def HOST = "localhost"
def PORT = 9577

def remoteAgents = RemoteAgents.create()

def remoteAgent = remoteAgents.get HOST, PORT, "agent" get()
remoteAgent.executionPolicy = AgentClosureExecutionPolicy.LOCAL

println "Agent value: ${remoteAgent.val}"

def x = "local-value"

remoteAgent << {
    println "updating..."
    updateValue x
}
sleep 500

println "Agent value: ${remoteAgent.val}"
