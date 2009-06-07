package org.gparallelizer.actors

import org.gparallelizer.actors.Actor;

/**
 * An internal representation of received messages holding both the original message plus the sender actor reference.
 * This class is not intented to be use directly by users.
 *
 * @author Vaclav Pech
 * Date: Feb 27, 2009
 */
final class ActorMessage {
    final Object payLoad
    final Actor sender

    /**
     * Creates a new instance
     * @param payLoad The original message
     * @param sender The sending actor, null, if the message was not sent by an actor
     */
    private ActorMessage(final Object payLoad, final Actor sender) {
        this.payLoad = payLoad;
        this.sender = sender;
    }

    /**
     * Factory method to create instances of ActorMessage with given payload.
     * The sender of the ActorMessage is retrieved from the ReplyRegistry.
     * * @param payLoad The original message
     */
    public static ActorMessage build(final Object payLoad, final boolean enhanceForReplies) {
        new ActorMessage(payLoad, enhanceForReplies ? ReplyRegistry.threadBoundActor() : null)
    }

    public String toString() { "Message from $sender: $payLoad" }
}
