package org.gparallelizer.dataflow

import org.gparallelizer.actors.pooledActors.PooledActor

/**
 * Contains factory methods to create dataflow actors and starting them.
 *
 * @author Vaclav Pech, Dierk Koenig
 * Date: Jun 4, 2009
 */
public abstract class DataFlow {

    /**
     * Creates a new instance of SingleRunActor to run the supplied code.
     */
    public static PooledActor start(final Closure code) {
        new SingleRunActor(body: code).start()
    }
}