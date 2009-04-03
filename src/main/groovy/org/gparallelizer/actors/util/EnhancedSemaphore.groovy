package org.gparallelizer.actors.util

import java.util.concurrent.Semaphore

/**
 * Extends Semaphore with a handy withSemaphore(Closure) method to safely acquire and release the Semaphore
 * for the passed-in closure.
 * Use:
 * def extendedSemaphore = new ExtendedSemaphore()
 * extendedSemaphore.withSemaphore() {
 *      //sepaphore acquired here
 * }
 * 
 * @author Vaclav Pech
 * Date: Jan 8, 2009
 */
public class EnhancedSemaphore extends Semaphore {

    /**
     * Creates a new EnhancedSemaphore, delegating to the Semaphore class constructor.
     * @param permits Maximum number of concurrently accepted threads.
     */
    def EnhancedSemaphore(final int permits) {
        super(permits);
    }

    /**
     * Performs the passed-in closure with the Semaphore acquired and releases the Semaphore automatically
     * after the closure finishes.
     * @param cl The closure to perform with the Semaphore acquired
     */
    public void withSemaphore(Closure cl) {
        acquire()
        try {
            cl.call()
        } finally {
            release()
        }
    }
}