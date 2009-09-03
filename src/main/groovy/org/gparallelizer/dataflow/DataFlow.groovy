package org.gparallelizer.dataflow

import org.gparallelizer.actors.pooledActors.PooledActor
import org.gparallelizer.dataflow.SingleRunThread

/**
 * Contains factory methods to create Dataflow Concurrency threads.
 *
 * @author Vaclav Pech
 * Date: Jun 4, 2009
 */
public abstract class DataFlow {

    /**
     * Creates a new instance of SingleRunThread to run the supplied code.
     */
    public static PooledActor thread(final Closure code) {
        new SingleRunThread(body: code).start()
    }
}