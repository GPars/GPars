package org.gparallelizer.dataflow;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * Represents a thread-safe single-assignment, multi-read variable.
 * Each instance of DataFlowVariable can be read repeatedly any time using the '()' operator and assigned once
 * in its lifetime using the '<<' operator. Reads preceding assignment will be blocked until the value
 * is assigned.
 *
 * By implementation/performance reasons this class is inherited from AbstractQueuedSynchronizer. Users
 * of the class SHOULD NOT take this inheritance in to consideration, as it can be changed in the future
 *
 * @author Vaclav Pech, Alex Tkachman
 * Date: Jun 4, 2009
 * @param <T> Type of values to bind with the DataFlowVariable
 */
public final class DataFlowVariable<T> extends AbstractQueuedSynchronizer {
    private volatile T value;

    private static final int S_NOT_INITIALIZED = 0;
    private static final int S_INITIALIZING    = 1;
    private static final int S_INITIALIZED     = 2;

    public DataFlowVariable () {
        setState(S_NOT_INITIALIZED);
    }

    /**
      * Reads the value of the variable. Blocks, if the value has not been assigned yet.
     * @return The actual value
     * @throws InterruptedException If the current thread gets interrupted while waiting for the variable to be bound
     */
    public T getVal() throws InterruptedException {
        acquireSharedInterruptibly(1);
        return value;
    }

    /**
     * Assigns a value to the variable. Can only be invoked once on each instance of DataFlowVariable
     * @param value The value to assign
     */
    public final void bind(final T value) {
        if (getState() == S_NOT_INITIALIZED) {
            if (compareAndSetState(S_NOT_INITIALIZED, S_INITIALIZING)) {
                this.value = value;
                setState(S_INITIALIZED);
                releaseShared(1);
                return;
            }
        }
        throw new IllegalStateException("A DataFlowVariable can only be assigned once.");
    }

    /**
     * Assigns a value to the variable. Can only be invoked once on each instance of DataFlowVariable
     * @param value The value to assign
     */
    public void leftShift(final T value) {
        bind(value);
    }

    /**
     * Assigns a value from one DataFlowVariable instance to this variable.
     * Can only be invoked once on each instance of DataFlowVariable
     * @param ref The DataFlowVariable instance the value of which to bind
     * @throws InterruptedException If the current thread gets interrupted while waiting for the variable to be bound
     */
    public void leftShift(final DataFlowVariable<T> ref) throws InterruptedException {
        bind(ref.getVal());
    }

    @Override public String toString() {
        return "DataFlowVariable(value=" + value + ')';
    }

    protected int tryAcquireShared(int acquires) {
        return getState() == S_INITIALIZED ? 1 : -1;
    }

    protected boolean tryReleaseShared(int releases) {
        if (getState() != S_INITIALIZED) {
            throw new IllegalStateException("State broken");
        }
        return true;
    }
}