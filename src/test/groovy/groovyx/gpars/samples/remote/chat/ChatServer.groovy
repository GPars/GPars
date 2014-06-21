package groovyx.gpars.samples.remote.chat

import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.DefaultActor
import groovyx.gpars.actor.remote.RemoteActors

class ServerActor extends DefaultActor {
    Map<String, Actor> connectedClients = new HashMap<>();

    @Override
    protected void act() {
        loop {
            react { message ->
                println message
            }
        }
    }
}

server = new ServerActor()
server.start()

RemoteActors.register(server, "chat-server")

server.join()
