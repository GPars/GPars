package groovyx.gpars.samples.remote.agent

import groovyx.gpars.agent.remote.RemoteAgents

println "Remote Agent - remote"

def HOST = "localhost"
def PORT = 9577

def remoteAgent = RemoteAgents.get HOST, PORT, "agent", null get()

println "Agent value: ${remoteAgent.val}"