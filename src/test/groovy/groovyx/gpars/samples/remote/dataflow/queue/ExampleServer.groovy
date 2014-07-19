package groovyx.gpars.samples.remote.dataflow.queue

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.remote.RemoteDataflows
import groovyx.gpars.remote.LocalHost
import groovyx.gpars.remote.netty.NettyTransportProvider

def HOST = "localhost"
def PORT = 9101

LocalHost localHost = new LocalHost()
NettyTransportProvider.startServer HOST, PORT, localHost

def queue = new DataflowQueue()

RemoteDataflows.publish queue, "queue"

sleep 10000

println "Sending..."
queue << "a"
queue << "b"
queue << "c"