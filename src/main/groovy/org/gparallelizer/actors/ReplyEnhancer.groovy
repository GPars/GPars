package org.gparallelizer.actors;

/**
 * Enables actors and messages to send replies.
 *
 * @author Vaclav Pech
 * Date: Apr 15, 2009
 */
public abstract class ReplyEnhancer {

    /**
     * Adds reply() and replyIfExists() methods to the currentActor and the message.
     * These methods will call send() on the target actor (the sender of the original message)
     * @param actor The actor to enhance
     * @param message The instance of ActorMessage wrapping the sender actor, who we need to be able to respond to,
     * plus the original message
     */
    public static void enhanceWithReplyMethods(final Actor actor, final ActorMessage message) {
        final Actor sender = message.sender
        enhanceObject(actor, sender)
        enhanceObject(message.payLoad, sender)
    }

    /**
     * Adds reply() and replyIfExists() methods to the currentActor and all the messages. Replies invoked on the actor
     * will be sent to the sender of the last message in the supplied list of messages.
     * These methods will call send() on the target actor (the sender of the original message)
     * @param actor The actor to enhance
     * @param message The instance of ActorMessage wrapping the sender actor, who we need to be able to respond to,
     * plus the original message
     */
    public static void enhanceWithReplyMethods(final Actor actor, final List<ActorMessage> messages) {
        messages.each {ActorMessage message ->
            final Actor sender = message.sender
            enhanceObject(message.payLoad, sender)
        }
        enhanceObject(actor, messages[-1].sender)
    }

    /**
     * Enhances the replier's metaClass with reply() and replyIfExists() methods to send messages to the sender
     */
    private static def enhanceObject(def replier, Actor sender) {
        replier.metaClass.reply = {msg ->
            if (sender) {
                sender.send msg
            } else {
                throw new IllegalArgumentException("Cannot send a message ${it} to a null recipient.")
            }
        }

        replier.metaClass.replyIfExists = {msg ->
            try {
                sender?.send msg
            } catch (IllegalStateException ignore) { }
        }
    }
}