package groovyx.gpars.samples.remote.actor.chat

import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.DefaultActor
import groovyx.gpars.actor.remote.RemoteActors

class ChatClientActor extends DefaultActor {
    String name
    def serverPromise

    def consoleActor = Actors.actor {
        loop {
            react { ChatMessage message ->
                println "${message.sender}: ${message.message}"
            }
        }
    }

    ChatClientActor(def serverPromise, String name) {
        this.serverPromise = serverPromise
        this.name = name
    }

    public afterStop(List<Object> messages) {
        def server = serverPromise.get()
        server << new ChatMessage(action: "unregister", sender: name)
    }

    @Override
    protected void act() {
        def server = serverPromise.get()

        // register
        server << new ChatMessage(action: "register", sender: name, message: consoleActor)

        loop {
            react { line ->
                if (line == "@show") {
                    server << new ChatMessage(action: "show", sender: name)
                }
                else {
                    server << new ChatMessage(action: "say", sender: name, message: line)
                }
            }
        }
    }
}
