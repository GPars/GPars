package org.gparallelizer.actors.util;

import java.util.concurrent.CountDownLatch;

/**
 * Wraps CountDownLatch when ExecutorService is used.
 *
 * @author Vaclav Pech
 * Date: Jun 16, 2009
 */
public final class LatchActorBarrier extends ActorBarrier {
    private final CountDownLatch latch = new CountDownLatch(1);

    /**
     * Indicates accomplished work
     */
    @Override public void done() { latch.countDown(); }

    /**
     * Waits for work completion
     * @throws InterruptedException If the thread gets interrupted while waiting
     */
    @Override public void awaitCompletion() throws InterruptedException { latch.await(); }
}