package groovyx.gpars.samples.remote.dataflow.broadcast

import groovyx.gpars.dataflow.DataflowBroadcast
import groovyx.gpars.dataflow.remote.RemoteDataflows
import groovyx.gpars.remote.netty.NettyTransportProvider

def HOST = "localhost"
def PORT = 9101

NettyTransportProvider.startServer HOST, PORT

def broadcastStream = new DataflowBroadcast()

RemoteDataflows.publish broadcastStream, "broadcast"

sleep 10000

println "Sending..."
broadcastStream << "Message 1"
broadcastStream << "Message 2"
broadcastStream << "Message 3"