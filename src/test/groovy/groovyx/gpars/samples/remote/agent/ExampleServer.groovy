package groovyx.gpars.samples.remote.agent

import groovyx.gpars.agent.Agent
import groovyx.gpars.agent.remote.RemoteAgents
import groovyx.gpars.remote.LocalHost
import groovyx.gpars.remote.netty.NettyTransportProvider

println "Remote Agent - local"

def HOST = "localhost"
def PORT = 9577

LocalHost localHost = new LocalHost()
NettyTransportProvider.startServer HOST, PORT, localHost

def agent = new Agent("state")
RemoteAgents.publish agent, "agent"