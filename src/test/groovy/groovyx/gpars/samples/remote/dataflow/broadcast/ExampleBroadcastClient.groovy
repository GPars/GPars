package groovyx.gpars.samples.remote.dataflow.broadcast

import groovyx.gpars.dataflow.remote.RemoteDataflows

def HOST = "localhost"
def PORT = 9101

println "Example: DataflowBroadcast"

def remoteDataflows = RemoteDataflows.create()

def streamPromise = remoteDataflows.getReadChannel HOST, PORT, "broadcast"
def stream = streamPromise.get()

println "Value from stream: ${stream.val}"
println "Value from stream: ${stream.val}"
println "Value from stream: ${stream.val}"