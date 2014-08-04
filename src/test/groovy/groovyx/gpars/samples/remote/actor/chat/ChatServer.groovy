package groovyx.gpars.samples.remote.actor.chat

import groovyx.gpars.actor.remote.RemoteActors

def HOST = "localhost"
def PORT = 9000

def remoteActors = RemoteActors.create()
remoteActors.startServer HOST, PORT

println "Chat server"

server = new ChatServerActor()
server.start()

remoteActors.publish server, "chat-server"

server.join()
