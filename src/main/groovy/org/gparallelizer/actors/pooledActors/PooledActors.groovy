package org.gparallelizer.actors.pooledActors

/**
 * Provides handy helper methods to create pooled actors and customize the underlying thread pool.
 * Use static import to be able to call PooledActors methods without the need to prepend them with the PooledActors identifier.
 * <pre>
 * import static org.gparallelizer.actors.pooledActors.PooledActors.*
 *
 * PooledActors.defaultPooledActorGroup.resize 1
 *
 * def actor = actor {
 *     react {message ->
 *         println message
 *     }
 *     //this line will never be reached
 * }.start()
 *
 * actor.send 'Hi!'
 * </pre>
 *
 * All actors created through the PooledActors class will belong to the same default actor group and run
 * on daemon threads.
 * The PooledActorGroup class should be used when actors need to be grouped into multiple groups or when non-daemon
 * threads are to be used.
 * @author Vaclav Pech
 * Date: Feb 18, 2009
 */
public abstract class PooledActors {

    /**
     * The default actor group to share by all actors created through the PooledActors class.
     */
    public final static PooledActorGroup defaultPooledActorGroup = new PooledActorGroup()

    /**
     * Creates a new instance of PooledActor, using the passed-in closure as the body of the actor's act() method.
     * The created actor will be part of the default actor group.
     * @param handler The body of the newly created actor's act method.
     * @return A newly created instance of the AbstractPooledActor class
     */
    public static AbstractPooledActor actor(Closure handler) {
        return defaultPooledActorGroup.actor(handler)
    }
}