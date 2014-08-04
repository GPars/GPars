package groovyx.gpars.samples.remote.dataflow.queuebalancer

import groovyx.gpars.dataflow.remote.RemoteDataflows

def HOST = "localhost"
def PORT = 9111

println "Example: DataflowQueue load balancer"

def remoteDataflows = RemoteDataflows.create()
def stream = remoteDataflows.getDataflowQueue HOST, PORT, "queue-balancer" get()

def processedTasks = 0

while (true) {
    def task = stream.val
    processedTasks += 1
    println "#tasks: ${processedTasks}, last-task: ${task}"
}
