package org.gparallelizer.actors

import org.gparallelizer.actors.Actor
import org.gparallelizer.actors.ActorMessage;

/**
 * Enables messages to send replies.
 *
 * @author Vaclav Pech
 * Date: Jun 9, 2009
 */
public abstract class ReplyEnhancer {
    /**
     * Adds reply() and replyIfExists() methods to all the messages.
     * These methods will call send() on the target actor (the sender of the original message).
     * @param messages The instance of ActorMessage wrapping the sender actor, which we need to be able to respond to,
     * plus the original message
     */
    public static void enhanceWithReplyMethodsToMessages(final List<ActorMessage> messages) {
        for (final ActorMessage message: messages) {
            if (message != null) {
                enhanceObject(message.payLoad, message.sender);
            }
        }
    }

    /**
     * Enhances the replier's metaClass with reply() and replyIfExists() methods to send messages to the sender
     */
    private static def enhanceObject(final def replier, final Actor sender) {
        //call to getMetaClass() is required, since maps don't handle metaClass property access correctly
        replier.getMetaClass().reply = {msg ->
            if (sender != null) sender.send msg
            else throw new IllegalArgumentException("Cannot send a reply message ${msg} to a null recipient.")
        }

        replier.getMetaClass().replyIfExists = {msg ->
            try {
                sender?.send msg
            } catch (IllegalStateException ignore) { }
        }
    }
}
