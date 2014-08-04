package groovyx.gpars.samples.remote.dataflow.queue

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.remote.RemoteDataflows

def HOST = "localhost"
def PORT = 9101

println "Example: DataflowQueue"

def remoteDataflows = RemoteDataflows.create()
remoteDataflows.startServer HOST, PORT

def queue = new DataflowQueue()

remoteDataflows.publish queue, "queue"

println "Waiting..."
sleep 10000
println "Sending..."
queue << "a"
queue << "b"
queue << "c"