package groovyx.gpars.samples.remote.dataflow.broadcast

import groovyx.gpars.dataflow.remote.RemoteDataflows

def HOST = "localhost"
def PORT = 9101

def stream = RemoteDataflows.getReadChannel HOST, PORT, "broadcast" get()

stream << "Client message 1"

println stream.val
println stream.val
println stream.val
println stream.val