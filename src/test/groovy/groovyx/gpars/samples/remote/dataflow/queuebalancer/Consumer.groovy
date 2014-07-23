package groovyx.gpars.samples.remote.dataflow.queuebalancer

import groovyx.gpars.dataflow.remote.RemoteDataflows

def HOST = "localhost"
def PORT = 9111

def stream = RemoteDataflows.getDataflowQueue HOST, PORT, "queue-balancer" get()

def processedTasks = 0

while (true) {
    def task = stream.val
    processedTasks += 1
    println "#tasks: ${processedTasks}"
}
