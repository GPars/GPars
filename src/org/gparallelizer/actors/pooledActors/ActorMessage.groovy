package org.gparallelizer.actors.pooledActors

import org.gparallelizer.actors.Actor;

/**
 *
 * @author Vaclav Pech
 * Date: Feb 27, 2009
 */
final class ActorMessage {
    final Object payLoad
    final Actor sender

    public ActorMessage(final Object payLoad, final Actor sender) {
        this.payLoad = payLoad;
        this.sender = sender;
    }

    public String toString() { "Message from $sender: $payLoad" }
}
