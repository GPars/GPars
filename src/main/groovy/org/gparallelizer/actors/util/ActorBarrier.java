package org.gparallelizer.actors.util;

/**
 * Unifies CountDownLatch and TaskBarrier under one interface to allow easy switch between ExecutorService and ForkJoin
 * and yet leverage the Fork/Join TaskBarrier class, which can allow waiting threads perform other tasks by work-stealing..
 *
 * @author Vaclav Pech
 * Date: Jun 16, 2009
 */
@SuppressWarnings({"MethodReturnOfConcreteClass"})
public abstract class ActorBarrier {

    /**
     * Indicates accomplished work
     */
    public abstract void done();

    /**
     * Waits for work completion
     * @throws InterruptedException If the thread gets interrupted while waiting
     */
    public abstract void awaitCompletion() throws InterruptedException;

    /**
     * Creates the appropriate ActorBarrier implementation based on the fjUsed flag.
     * @param fjUsed Indicates, whether Fork/Join is being used
     * @return The appropriate ActorBarrier implementation
     */
    public static ActorBarrier create(final boolean fjUsed) {
        if (fjUsed) {
            return new FJActorBarrier();
        } else {
            return new LatchActorBarrier();
        }
    }
}
