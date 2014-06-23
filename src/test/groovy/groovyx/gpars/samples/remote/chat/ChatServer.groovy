package groovyx.gpars.samples.remote.chat

import groovyx.gpars.actor.remote.RemoteActors

println "Chat server"

server = new ChatServerActor()
server.start()

RemoteActors.register(server, "chat-server")

server.join()
