package groovyx.gpars.samples.remote.agent

import groovyx.gpars.agent.Agent
import groovyx.gpars.agent.remote.RemoteAgents

println "Remote Agent - local"

def HOST = "localhost"
def PORT = 9577

def remoteAgents = RemoteAgents.create()
remoteAgents.startServer HOST, PORT

def agent = new Agent("state")
remoteAgents.publish agent, "agent"
