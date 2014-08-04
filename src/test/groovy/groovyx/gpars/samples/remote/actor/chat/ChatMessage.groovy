package groovyx.gpars.samples.remote.actor.chat

class ChatMessage implements Serializable {
    String action
    String sender
    Object message
}
