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
     * These methods will call send() on the target actor (the sender of the original message).
     * @param actor The actor to enhance
     * @param message The instance of ActorMessage wrapping the sender actor, who we need to be able to respond to,
     * plus the original message
     */
    public static void enhanceWithReplyMethods(final Actor actor, final ActorMessage message) {
        assert actor!=null
        final Actor sender = message?.sender
        enhanceObject(actor, [sender])
        if (message) enhanceObject(message.payLoad, [sender])
    }

    /**
     * Adds reply() and replyIfExists() methods to the currentActor and all the messages.
     * These methods will call send() on the target actor (the sender of the original message).
     * Replies invoked on the actor will be sent to the senders of all the messages.
     * @param actor The actor to enhance
     * @param message The instance of ActorMessage wrapping the sender actor, who we need to be able to respond to,
     * plus the original message
     */
    public static void enhanceWithReplyMethodsToMessages(final Actor actor, final List<ActorMessage> messages) {
        assert actor!=null
        messages.each { if(it) enhanceObject(it.payLoad, [it?.sender]) }
        enhanceObject(actor, messages*.sender)
    }

    /**
     * Enhances the replier's metaClass with reply() and replyIfExists() methods to send messages to the sender
     */
    private static def enhanceObject(final def replier, final List<Actor> senders) {
        //todo getMetaClass() is required, since maps don't handle metaClass property access correctly
        replier.getMetaClass().reply = {msg ->
            if (!senders.isEmpty()) {
                for(sender in senders) {
                    if (sender) sender.send msg
                    else throw new IllegalArgumentException("Cannot send a reply message ${msg} to a null recipient.")
                }
            } else {
                throw new IllegalArgumentException("Cannot send replies. The list of recipients is empty.")
            }
        }

        replier.getMetaClass().replyIfExists = {msg ->
            try {
                for(sender in senders) sender?.send msg
            } catch (IllegalStateException ignore) { }
        }
    }
}