package groovyx.gpars.samples.remote.dataflow.variable

import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.dataflow.remote.RemoteDataflows

def HOST = "localhost"
def PORT = 9009

println "Example: DataflowVariable"

def remoteDataflows = RemoteDataflows.create()
remoteDataflows.startServer HOST, PORT

def var1 = new DataflowVariable()
def var2 = new DataflowVariable()

remoteDataflows.publish var1, "variabledemo-var-GPars"
remoteDataflows.publish var2, "variabledemo-var-Dataflows"

println "Before bind"
sleep 10000
var1 << "GPars"
var2 << "Remote Dataflows"
println "After bind"