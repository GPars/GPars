package groovyx.gpars.samples.remote.dataflow.variable

import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.dataflow.remote.RemoteDataflows
import groovyx.gpars.remote.LocalHost
import groovyx.gpars.remote.netty.NettyTransportProvider

def HOST = "localhost"
def PORT = 9009

println "Example: DataflowVariable"

LocalHost localHost = new LocalHost()
NettyTransportProvider.startServer HOST, PORT, localHost

def var1 = new DataflowVariable()
def var2 = new DataflowVariable()

RemoteDataflows.publish var1, "variabledemo-var-GPars"
RemoteDataflows.publish var2, "variabledemo-var-Dataflows"

println "Before bind"
sleep 10000
var1 << "GPars"
var2 << "Remote Dataflows"
println "After bind"

// NettyTransportProvider.stopServer()

