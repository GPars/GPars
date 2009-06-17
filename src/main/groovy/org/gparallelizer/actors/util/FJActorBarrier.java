package org.gparallelizer.actors.util;

import jsr166y.forkjoin.TaskBarrier;

/**
 * Wraps JSR-166y-specific TaskBarrier.
 * 
 * @author Vaclav Pech
 * Date: Jun 16, 2009
 */
public final class FJActorBarrier extends ActorBarrier {
    private final TaskBarrier barrier = new TaskBarrier(2);

    /**
     * Indicates accomplished work
     */
    @Override public void done() { barrier.arriveAndDeregister(); }

    /**
     * Waits for work completion
     */
    @Override public void awaitCompletion() { barrier.arriveAndAwait(); }
}