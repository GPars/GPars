package org.gparallelizer.actors

import java.util.concurrent.ThreadFactory

/**
 * Provides logical grouping for actors. Each group holds a thread factory, which will create threads for all
 * actors created in that group. Actors created through the ActorGroup.actor*() methods will automatically belong
 * to the group through which they were created.
 * <pre>
 *
 * def group = new ActorGroup()
 *
 * def actor = group.actor {
 *     receive {message ->
 *         println message
 *     }
 * }.start()
 *
 * actor.send 'Hi!'
 * ...
 * actor.stop()
 * </pre>
 *
 * Otherwise, if constructing Actors directly through their constructors, the AbstractActor.actorGroup property,
 * which defaults to the Actors.defaultActorGroup, can be set before the actor is started.
 *
 * <pre>
 * def group = new ActorGroup(false)
 *
 * def actor = new MyActor()
 * actor.actorGroup = group
 * actor.start()
 *
 * </pre>
 *
 * @author Vaclav Pech
 * Date: May 8, 2009
 */
public class ActorGroup {
    /**
     * Stored the group actors' thread pool
     */
    final ThreadFactory threadFactory

    /**
     * Creates a group of actors. The actors will share a common thread pool of non-daemon threads.
     */
    def ActorGroup() { this(false) }

    /**
     * Creates a group of actors. The actors will share a common thread pool.
     * @param daemon Sets the daemon flag of threads in the group's thread pool.
     */
    def ActorGroup(final boolean daemon) {
        threadFactory = {Runnable r ->
            final Thread thread = new Thread(r)
            thread.daemon = daemon
            return thread
        } as ThreadFactory
    }

    /**
     * Creates a new instance of DefaultActor, using the passed-in closure as the body of the actor's act() method.
     */
    public final Actor actor(Closure handler) {
        defaultActor(handler)
    }

    /**
     * Creates a new instance of DefaultActor, using the passed-in closure as the body of the actor's act() method.
     * The actor will stop after one iteration through the passed-in closure.
     */
    public final Actor oneShotActor(Closure handler) {
        defaultOneShotActor(handler)
    }

    /**
     * Creates a new instance of DefaultActor, using the passed-in closure as the body of the actor's act() method.
     */
    public final Actor defaultActor(Closure handler = {throw new UnsupportedOperationException()}) {
        final Actor actor = [act: handler] as DefaultActor
        handler.delegate = actor
        actor.actorGroup = this
        return actor
    }

    /**
     * Creates a new instance of DefaultActor, using the passed-in closure as the body of the actor's act() method.
     * The actor will stop after one iteration through the passed-in closure.
     */
    public final Actor defaultOneShotActor(Closure handler = {throw new UnsupportedOperationException()}) {
        Closure enhancedHandler = enhanceOneShotHandler(handler)
        final DefaultActor actor = [act: enhancedHandler] as DefaultActor
        handler.delegate = actor
        enhancedHandler.delegate = actor
        actor.actorGroup = this
        return actor
    }

    /**
     * Creates a new instance of SynchronousActor, using the passed-in closure as the body of the actor's act() method.
     */
    public final Actor synchronousActor(Closure handler = {throw new UnsupportedOperationException()}) {
        final Actor actor = [act: handler] as SynchronousActor
        handler.delegate = actor
        actor.actorGroup = this
        return actor
    }

    /**
     * Creates a new instance of SynchronousActor, using the passed-in closure as the body of the actor's act() method.
     * The actor will stop after one iteration through the passed-in closure.
     */
    public final Actor synchronousOneShotActor(Closure handler = {throw new UnsupportedOperationException()}) {
        Closure enhancedHandler = enhanceOneShotHandler(handler)
        final Actor actor = [act: enhancedHandler] as SynchronousActor
        handler.delegate = actor
        enhancedHandler.delegate = actor
        actor.actorGroup = this
        return actor
    }

    /**
     * Creates a new instance of BoundedActor, using the passed-in closure as the body of the actor's act() method.
     */
    public final Actor boundedActor(Closure handler = {throw new UnsupportedOperationException()}) {
        return new InlinedBoundedActor(this, handler)
    }

    /**
     * Creates a new instance of BoundedActor, using the passed-in closure as the body of the actor's act() method.
     */
    public final Actor boundedActor(int capacity, Closure handler = {throw new UnsupportedOperationException()}) {
        return new InlinedBoundedActor(capacity, handler)
    }

    /**
     * Creates a new instance of BoundedActor, using the passed-in closure as the body of the actor's act() method.
     * The actor will stop after one iteration through the passed-in closure.
     */
    public final Actor boundedOneShotActor(Closure handler = {throw new UnsupportedOperationException()}) {
        Closure enhancedHandler = enhanceOneShotHandler(handler)
        Actor actor = new InlinedBoundedActor(this, enhancedHandler)
        handler.delegate = actor
        actor.actorGroup = this
        return actor
    }

    /**
     * Creates a new instance of BoundedActor, using the passed-in closure as the body of the actor's act() method.
     * The actor will stop after one iteration through the passed-in closure.
     */
    public final Actor boundedOneShotActor(int capacity, Closure handler = {throw new UnsupportedOperationException()}) {
        Closure enhancedHandler = enhanceOneShotHandler(handler)
        Actor actor = new InlinedBoundedActor(capacity, enhancedHandler)
        handler.delegate = actor
        actor.actorGroup = this
        return actor
    }

    private static Closure enhanceOneShotHandler(Closure handler) {
        assert handler != null
        return {
            try {
                handler()
            } finally {
                stop()
            }
        }
    }
}

final class InlinedBoundedActor extends BoundedActor {

    final Closure handler

    def InlinedBoundedActor(ActorGroup actorGroup, Closure handler) {
        this.handler = handler
        this.actorGroup = actorGroup
        handler.delegate = this
    }

    def InlinedBoundedActor(final int capacity, Closure handler) {
        super(capacity);
        this.handler = handler
        handler.delegate = this
    }

    @Override
    protected void act() {
        handler.call()
    }
}