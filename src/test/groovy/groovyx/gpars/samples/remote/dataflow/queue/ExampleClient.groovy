package groovyx.gpars.samples.remote.dataflow.queue

import groovyx.gpars.dataflow.remote.RemoteDataflows

def HOST = "localhost"
def PORT = 9101

def stream = RemoteDataflows.getDataflowQueue HOST, PORT, "queue" get()

println stream.val
println stream.val

stream << "xyz"

println stream.val
println stream.val