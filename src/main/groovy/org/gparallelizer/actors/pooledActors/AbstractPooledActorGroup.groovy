package org.gparallelizer.actors.pooledActors

import org.gparallelizer.actors.AbstractActorGroup

/**
 * Provides a common super class fo pooled actor's groups.
 *
 * @author Vaclav Pech
 * Date: May 8, 2009
 */
public abstract class AbstractPooledActorGroup extends AbstractActorGroup {

    def AbstractPooledActorGroup() { }

    def AbstractPooledActorGroup(final boolean usedForkJoin) { super(usedForkJoin); }

    /**
     * Creates a new instance of PooledActor, using the passed-in closure as the body of the actor's act() method.
     * The created actor will belong to the pooled actor group.
     * @param handler The body of the newly created actor's act method.
     * @return A newly created instance of the AbstractPooledActor class
     */
    public final AbstractPooledActor actor(Closure handler) {
        final AbstractPooledActor actor = [act: handler] as AbstractPooledActor
        handler.resolveStrategy=Closure.DELEGATE_FIRST
        handler.delegate = actor
        actor.actorGroup = this
        return actor
    }

    /**
     * Creates a reactor around the supplied code.
     * When a reactor receives a message, the supplied block of code is run with the message
     * as a parameter and the result of the code is send in reply.
     * @param The code to invoke for each received message
     * @return A new instance of ReactiveEventBasedThread
     */
    public final AbstractPooledActor reactor(final Closure code) {
        new ReactiveActor(body: code)
    }
}