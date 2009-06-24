package org.gparallelizer.dataflow;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;

/**
 * Represents a thread-safe single-assignment, multi-read variable.
 * Each instance of DataFlowVariable can be read repetadly any time using the '()' operator and assigned once
 * in its lifetime using the '<<' operator. Reads preceding assignment will be blocked until the value
 * is assigned.
 * Implemented using a CountDownLatch.
 *
 * @author Vaclav Pech
 * Date: Jun 4, 2009
 * @param <T> Type of values to bind with the DataFlowVariable
 */
public final class DataFlowVariable<T> {
    private final AtomicReference<T> value = new AtomicReference<T>();
    private final CountDownLatch latch = new CountDownLatch(1);

    /**
      * Reads the value of the variable. Blocks, if the value has not been assigned yet.
     * @return The actual value
     * @throws InterruptedException If the current thread gets interrupted while waiting for the variable to be bound
     */
    public T getVal() throws InterruptedException {
        latch.await();
        return value.get();
    }

    /**
     * Assigns a value to the variable. Can only be invoked once on each instance of DataFlowVariable
     * @param value The value to assign
     */
    public void bind(final T value) {
        if (this.value.compareAndSet(null, value)) {
            latch.countDown();
        } else {
            throw new IllegalStateException("A DataFlowVariable can only be assigned once.");
        }
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
}