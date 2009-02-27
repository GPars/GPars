package org.gparallelizer.actors.pooledActors

/**
 * Provides handy helper methods to create pooled actors and customize the underlying thread pool.
 *
 * @author Vaclav Pech
 * Date: Feb 18, 2009
 */
public class PooledActors {

    private static final Pool instance = new FJBasedPool()

    public static Pool getPool() {instance}

    /**
     * Creates a new instance of PooledActor, using the passed-in closure as the body of the actor's act() method.
     */
    public static AbstractPooledActor actor(Closure handler) {
        final AbstractPooledActor actor = [act: handler] as AbstractPooledActor
        handler.delegate = actor
        return actor
    }
}