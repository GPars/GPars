package groovyx.gpars.samples.remote.dataflow.example

import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.dataflow.remote.RemoteDataflows
import groovyx.gpars.remote.netty.NettyTransportProvider

def HOST = "localhost"
def PORT = 9009

NettyTransportProvider.startServer HOST, PORT

def var = new DataflowVariable()
RemoteDataflows.publish var, "example-var"

sleep 10000

var << "test"

// NettyTransportProvider.stopServer()

