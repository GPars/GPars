package groovyx.gpars.samples.remote.dataflow.variabledemo

import groovyx.gpars.dataflow.remote.RemoteDataflows
import groovyx.gpars.remote.netty.NettyTransportProvider

def HOST = "localhost"
def PORT = 9009

def var1Future = RemoteDataflows.get HOST, PORT, "variabledemo-var-GPars"
def var2Future = RemoteDataflows.get HOST, PORT, "variabledemo-var-Dataflows"

def var1 = var1Future.get()
def var2 = var2Future.get()

println "${var1.val} supports ${var2.val}"

NettyTransportProvider.stopClients()