package org.gparallelizer.actors.pooledActors

/**
 * Provides logical grouping for pooled actors. Each group has an underlying thread pool, which will perform actions
 * on behalf of the actors belonging to the group. Actors created through the PooledActorGroup.actor() method
 * will automatically belong to the group through which they were created.
 * <pre>
 *
 * def group = new PooledActorGroup()
 * group.threadPool.resize 1
 *
 * def actor = group.actor {
 *     react {message ->
 *         println message
 *     }
 * }.start()
 *
 * actor.send 'Hi!'
 * ...
 * group.threadPool.shutdown()
 * </pre>
 *
 * Otherwise, if constructing Actors directly through their constructors, the AbstractPooledActor.actorGroup property,
 * which defaults to the PooledActors.defaultPooledActorGroup, can be set before the actor is started.
 *
 * <pre>
 * def group = new PooledActorGroup(false)
 *
 * def actor = new MyActor()
 * actor.actorGroup = group
 * actor.start()
 * ...
 * group.threadPool.shutdown()
 *
 * </pre>
 *
 * @author Vaclav Pech
 * Date: May 4, 2009
 */
public class PooledActorGroup {

    /**
     * Stored the group actors' thread pool
     */
    final Pool threadPool

    /**
     * Creates a group of actors. The actors will share a common thread pool of non-daemon threads.
     */
    def PooledActorGroup() { this(false) }

    /**
     * Creates a group of actors. The actors will share a common thread pool.
     * @param daemon Sets the daemon flag of threads in the group's thread pool.
     */
    def PooledActorGroup(final boolean daemon) {
        threadPool = new DefaultPool(daemon)
    }

    /**
     * Creates a group of actors. The actors will share a common thread pool.
     * @param daemon Sets the daemon flag of threads in the group's thread pool.
     * @param poolSize The initial size of the underlying thread pool
     */
    def PooledActorGroup(final boolean daemon, final int poolSize) {
        threadPool = new DefaultPool(daemon, poolSize)
    }

    /**
     * Creates a new instance of PooledActor, using the passed-in closure as the body of the actor's act() method.
     * The created actor will belong to the pooled actor group.
     * @param handler The body of the newly created actor's act method.
     * @return A newly created instance of the AbstractPooledActor class
     */
    public final AbstractPooledActor actor(Closure handler) {
        final AbstractPooledActor actor = [act: handler] as AbstractPooledActor
        handler.delegate = actor
        actor.actorGroup = this
        return actor
    }
}