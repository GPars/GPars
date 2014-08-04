package groovyx.gpars.samples.remote.chat

import groovyx.gpars.actor.remote.RemoteActors
import groovyx.gpars.remote.netty.NettyTransportProvider

def HOST = "localhost"
def PORT = 9000

NettyTransportProvider.startServer(HOST, PORT)

println "Chat server"

server = new ChatServerActor()
server.start()

RemoteActors.register(server, "chat-server")

server.join()
