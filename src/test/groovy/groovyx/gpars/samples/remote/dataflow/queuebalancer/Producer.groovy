package groovyx.gpars.samples.remote.dataflow.queuebalancer

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.remote.RemoteDataflows
import groovyx.gpars.remote.LocalHost
import groovyx.gpars.remote.netty.NettyTransportProvider

def HOST = "localhost"
def PORT = 9111
def NUMBER_OF_TASKS = 100

println "Example: DataflowQueue load balancer"

def remoteDataflows = RemoteDataflows.create()
remoteDataflows.startServer HOST, PORT

def queue = new DataflowQueue()

remoteDataflows.publish queue, "queue-balancer"

println "Press any key to start..."
System.in.read()

(1..NUMBER_OF_TASKS).each { i ->
    queue << "task ${i}"
    sleep 50
}

