package org.gparallelizer.actors.pooledActors

/**
 * Provides handy helper methods to create pooled actors and customize the underlying thread pool.
 * Use static import to be able to call PooledActors methods without the need to prepend them with the PooledActors identifier.
 * <pre>
 * import static org.gparallelizer.actors.pooledActors.PooledActors.*
 *
 * getPool().resize 1
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
 * @author Vaclav Pech
 * Date: Feb 18, 2009
 */
public class PooledActors {

    /**
     * Stored the actors' thread pool
     */
    private static final Pool instance = new DefaultPool()

    /**
     * Returns the actors' thread pool
     * @return The thread pool shared by actors
     */
    public static Pool getPool() {instance}

    /**
     * Creates a new instance of PooledActor, using the passed-in closure as the body of the actor's act() method.
     * @param handler The body of the newly created actor's act method.
     * @return A newly created instance of the AbstractPooledActor class
     */
    public static AbstractPooledActor actor(Closure handler) {
        final AbstractPooledActor actor = [act: handler] as AbstractPooledActor
        handler.delegate = actor
        return actor
    }
}