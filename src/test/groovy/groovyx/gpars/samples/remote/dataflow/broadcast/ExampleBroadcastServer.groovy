package groovyx.gpars.samples.remote.dataflow.broadcast

import groovyx.gpars.dataflow.DataflowBroadcast
import groovyx.gpars.dataflow.remote.RemoteDataflows

def HOST = "localhost"
def PORT = 9101

println "Example: DataflowBroadcast"

def remoteDataflows = RemoteDataflows.create()
remoteDataflows.startServer HOST, PORT

def broadcastStream = new DataflowBroadcast()

remoteDataflows.publish broadcastStream, "broadcast"

println "Waiting..."
sleep 10000
println "Sending..."
broadcastStream << "Message 1"
broadcastStream << "Message 2"
broadcastStream << "Message 3"