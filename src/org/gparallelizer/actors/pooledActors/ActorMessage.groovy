package org.gparallelizer.actors.pooledActors

import org.gparallelizer.actors.Actor;

/**
 * An internal representation of received messages holding both the original message plus the sender actor reference.
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
    public ActorMessage(final Object payLoad, final Actor sender) {
        this.payLoad = payLoad;
        this.sender = sender;
    }

    public String toString() { "Message from $sender: $payLoad" }
}
