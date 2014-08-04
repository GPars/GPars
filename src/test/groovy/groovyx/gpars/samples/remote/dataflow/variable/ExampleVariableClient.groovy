package groovyx.gpars.samples.remote.dataflow.variable

import groovyx.gpars.dataflow.remote.RemoteDataflows

def HOST = "localhost"
def PORT = 9009

def remoteDataflows = RemoteDataflows.create()

def var1Promise = remoteDataflows.getVariable HOST, PORT, "variabledemo-var-GPars"
def var2Promise = remoteDataflows.getVariable HOST, PORT, "variabledemo-var-Dataflows"

def var1 = var1Promise.get()
def var2 = var2Promise.get()

println "${var1.val} supports ${var2.val}"