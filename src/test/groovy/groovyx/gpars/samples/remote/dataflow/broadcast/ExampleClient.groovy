package groovyx.gpars.samples.remote.dataflow.broadcast

import groovyx.gpars.dataflow.remote.RemoteDataflows

def HOST = "localhost"
def PORT = 9101

def stream = RemoteDataflows.getReadChannel HOST, PORT, "broadcast"

println stream.val
println stream.val
println stream.val