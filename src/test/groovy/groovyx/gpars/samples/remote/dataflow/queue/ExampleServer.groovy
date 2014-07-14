package groovyx.gpars.samples.remote.dataflow.queue

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.remote.RemoteDataflows
import groovyx.gpars.remote.netty.NettyTransportProvider

def HOST = "localhost"
def PORT = 9101

NettyTransportProvider.startServer HOST, PORT

def queue = new DataflowQueue()

RemoteDataflows.publish queue, "queue"

sleep 10000

println "Sending..."
println "TODO"