package groovyx.gpars.samples.remote.database

import groovyx.gpars.remote.netty.NettyTransportProvider

def HOST = "localhost"
def PORT = 9000

NettyTransportProvider.startServer(HOST, PORT)

println "Frontend"
