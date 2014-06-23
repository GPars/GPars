package groovyx.gpars.samples.remote.chat

import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.DefaultActor
import groovyx.gpars.actor.remote.RemoteActors

class ChatClientActor extends DefaultActor {
    String name
    def serverFuture

    def consoleActor = Actors.reactor { ChatMessage message ->
        println "${message.sender}: ${message.message}"
    }

    ChatClientActor(String host, int port, String name) {
        this.name = name
        serverFuture = RemoteActors.get(host, port, "chat-server")
    }

    @Override
    protected void act() {
        def server = serverFuture.get()

        // register
        server << new ChatMessage(action: "register", sender: name, message: consoleActor)

        loop {
            react { line ->
                server << new ChatMessage(action: "say", sender: name, message: line)
            }
        }
    }
}
