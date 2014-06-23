package groovyx.gpars.samples.remote.chat

import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.DefaultActor

class ChatServerActor extends DefaultActor {
    Map<String, Actor> connectedClients = new HashMap<>()

    @Override
    protected void act() {
        loop {
            react { message ->
                println message
                if (message instanceof ChatMessage) {
                    if (message.action == "say") {
                        connectedClients.each { name, client ->
                            if (name != message.sender) {
                                client << message
                            }
                        }
                    } else if (message.action == "register") {
                        connectedClients.each { name, client ->
                            client << new ChatMessage(action: "say", sender: "server", message: "${message.sender} has joined")
                        }
                        connectedClients.put(message.sender, message.message)
                    }
                }
            }
        }
    }
}
