package org.gparallelizer.actors.pooledActors;

/**
 *
 * @author Vaclav Pech
 * Date: Feb 27, 2009
 */
final class ActorMessage {
    final Object payLoad
    final PooledActor sender

    public ActorMessage(final Object payLoad, final PooledActor sender) {
        this.payLoad = payLoad;
        this.sender = sender;
    }

    public String toString() { "Message from $sender: $payLoad" }
}
