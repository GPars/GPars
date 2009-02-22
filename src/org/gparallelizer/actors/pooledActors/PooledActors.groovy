package org.gparallelizer.actors.pooledActors

/**
 * Provides handy helper methods to create various types of pooled actors and customize their behavior..
 *
 * @author Vaclav Pech
 * Date: Feb 18, 2009
 */
public class PooledActors {

    private static final Pool instance = new Pool()

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