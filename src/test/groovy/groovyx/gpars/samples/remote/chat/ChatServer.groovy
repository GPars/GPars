package groovyx.gpars.samples.remote.chat

import groovyx.gpars.actor.remote.RemoteActors


server = new ChatServerActor()
server.start()

RemoteActors.register(server, "chat-server")

server.join()
