package org.gparallelizer.dataflow;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;

public final class StraightDataFlowVariable<T> {
    private final AtomicReference<T> value = new AtomicReference<T>();
    private final CountDownLatch latch = new CountDownLatch(1);

    public T retrieve() throws InterruptedException {
        latch.await();
        return value.get();
    }

    public void bind(final T value) {
        if (this.value.compareAndSet(null, value)) {
            latch.countDown();
        } else {
            throw new IllegalStateException("A DataFlowVariable can only be assigned once.");
        }
    }

   /**
     * Reads the value of the variable. Blocks, if the value has not been assigned yet.
    * @return
    * @throws InterruptedException
    */
    public T bitwiseNegate() throws InterruptedException {
       return retrieve();
   }

    /**
     * Assigns a value to the variable. Can only be invoked once on each instance of DataFlowVariable
     * @param value
     */
    public void leftShift(final T value) {
        bind(value);
    }

    /**
     * Assigns a value from one DataFlowVariable instance to this variable.
     * Can only be invoked once on each instance of DataFlowVariable
     * @param ref
     * @throws InterruptedException
     */
    public void leftShift(final StraightDataFlowVariable<T> ref) throws InterruptedException {
        bind(ref.retrieve());
    }
}