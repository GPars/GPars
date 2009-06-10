package org.gparallelizer.actors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Enables actors and messages to send replies.
 *
 * @author Vaclav Pech
 * Date: Jun 9, 2009
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
        assert actor != null;
        if (!message.isEnhanceForReplies()) return;
        final Actor sender = message!=null ? message.getSender() : null;
        ReplyEnhancerHelper.enhanceObject(actor, Arrays.asList(sender));
        if (message!=null) ReplyEnhancerHelper.enhanceObject(message.getPayLoad(), Arrays.asList(sender));
    }

    /**
     * Adds reply() and replyIfExists() methods to the currentActor and all the messages.
     * These methods will call send() on the target actor (the sender of the original message).
     * Replies invoked on the actor will be sent to the senders of all the messages.
     * @param actor The actor to enhance
     * @param messages The instance of ActorMessage wrapping the sender actor, who we need to be able to respond to,
     * plus the original message
     */
    public static void enhanceWithReplyMethodsToMessages(final Actor actor, final List<ActorMessage> messages) {
        assert actor != null;
        for (final ActorMessage message : messages) {
            if (message != null && message.isEnhanceForReplies()) {
                ReplyEnhancerHelper.enhanceObject(message.getPayLoad(), Arrays.asList(message.getSender()));
            }
        }
        final List<Actor> senders = new ArrayList<Actor>();
        for(final ActorMessage message : messages) {
            //todo is the null check necessary?
            if (message==null || message.isEnhanceForReplies()) senders.add(message!=null ? message.getSender() : null);
        }
        if (!senders.isEmpty()) ReplyEnhancerHelper.enhanceObject(actor, senders);
    }

}
