package groovyx.gpars.samples.remote.chat

import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.DefaultActor

class ChatServerActor extends DefaultActor {
    Map<String, Actor> connectedClients = new HashMap<>()

    @Override
    protected void act() {
        loop {
            react { ChatMessage message ->
                switch (message.action) {
                    case "say":
                        def otherClients = connectedClients.findResults { name, client -> name != message.sender ? client : null }
                        otherClients.each { it << message }
                        break
                    case "register":
                        connectedClients.each { name, client -> client << new ChatMessage(action: "say", sender: "server", message: "${message.sender} has joined") }
                        connectedClients.put(message.sender, message.message)
                        break
                    case "unregister":
                        connectedClients.remove(message.sender)
                        connectedClients.each { name, client -> client << new ChatMessage(action: "say", sender: "server", message: "${message.sender} has left")}
                        break
                    case "show":
                        connectedClients[message.sender] << new ChatMessage(action: "show", sender: "server", message: connectedClients.keySet().toListString())
                        break
                }
            }
        }
    }
}
