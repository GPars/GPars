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


}