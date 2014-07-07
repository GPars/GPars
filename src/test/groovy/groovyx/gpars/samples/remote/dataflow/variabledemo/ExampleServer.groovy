package groovyx.gpars.samples.remote.dataflow.variabledemo

import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.dataflow.remote.RemoteDataflows
import groovyx.gpars.remote.netty.NettyTransportProvider

def HOST = "localhost"
def PORT = 9009

NettyTransportProvider.startServer HOST, PORT

def var1 = new DataflowVariable()
def var2 = new DataflowVariable()

RemoteDataflows.publish var1, "variabledemo-var-GPars"
RemoteDataflows.publish var2, "variabledemo-var-Dataflows"

sleep 10000

var1 << "GPars"
var2 << "Remote Dataflows"

// NettyTransportProvider.stopServer()

