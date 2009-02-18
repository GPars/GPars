package org.gparallelizer.actors.util

import java.util.concurrent.locks.ReentrantReadWriteLock


/**
 * Extends ReentrantReadWriteLock with handy withReadLock(Closure) and withWriteLock(Closure) methods to safely lock
 * and unlock the lock for the passed-in closure.
 * Use:
 * def extendedLock = new ExtendedRWLock()
 * extendedLock.withReadLock() {
 *      //read lock locked here
 * }
 *
 * @author Vaclav Pech
 * Date: Feb 18, 2009
 */
public class EnhancedRWLock extends ReentrantReadWriteLock {

    def EnhancedRWLock() { super() }

    def EnhancedRWLock(final fair) { super(fair); }

    /**
     * Performs the passed-in closure with the read lock locked and unlocks the read lock automatically
     * after the closure finishes.
     * @param cl The closure to perform with the read lock held
     */
    public void withReadLock(Closure cl) {
        readLock().lock()
        try {
            cl.call()
        } finally {
            readLock().unlock()
        }
    }

    /**
     * Performs the passed-in closure with the write lock locked and unlocks the write lock automatically
     * after the closure finishes.
     * @param cl The closure to perform with the write lock held
     */
    public void withWriteLock(Closure cl) {
        writeLock().lock()
        try {
            cl.call()
        } finally {
            writeLock().unlock()
        }
    }
}