package org.gparallelizer.dataflow;

import groovy.lang.Closure;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * Represents a thread-safe single-assignment, multi-read variable.
 * Each instance of DataFlowVariable can be read repeatedly any time using the '()' operator and assigned once
 * in its lifetime using the '<<' operator. Reads preceding assignment will be blocked until the value
 * is assigned.
 *
 * By implementation/performance reasons this class is inherited from AtomicInteger. Users
 * of the class SHOULD NOT take this inheritance in to consideration, as it can be changed in the future.
 * Changing of this value guarantees totally unpredictable behaviour.
 *
 * @author Vaclav Pech, Alex Tkachman
 * Date: Jun 4, 2009
 * @param <T> Type of values to bind with the DataFlowVariable
 */
public final class DataFlowVariable<T> {
    private volatile T value;

    private volatile int state;

    private volatile WaitingThread waiting;

    private static final int S_NOT_INITIALIZED = 0;
    private static final int S_INITIALIZED     = 1;

    private static final int T_NOT_ENQUEED = 0;
    private static final int T_ENQUEED     = 1;
    private static final int T_DEQUEED     = 2;

    private static class WaitingThread {
        final Thread thread;
        volatile WaitingThread previous;
        private final DataCallback callback;
        volatile int state;

        public WaitingThread(Thread thread, WaitingThread previous, DataCallback callback) {
            this.callback = callback;
            state = T_NOT_ENQUEED;
            this.thread = thread;
            this.previous = previous;
        }
    }

    public static interface DataCallback<T> {
        void onData (T data);
    }

    public DataFlowVariable () {
        state = S_NOT_INITIALIZED;
    }

    public void getVal(DataCallback<T> callback) {
        if (state != S_INITIALIZED) {
            WaitingThread newWaiting = new WaitingThread(null, null, callback);
            synchronized (this) {
                newWaiting.previous = waiting;
                waiting = newWaiting;
            }

            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (newWaiting) {
                if(newWaiting.state == T_NOT_ENQUEED) {
                    newWaiting.state = T_ENQUEED;
                    return;
                }
            }
        }

        scheduleCallback(callback);
    }

    /**
     * Reads the value of the variable. Blocks, if the value has not been assigned yet.
     * @return The actual value
     * @throws InterruptedException If the current thread gets interrupted while waiting for the variable to be bound
     */
    public T getVal() throws InterruptedException {
        if (state != S_INITIALIZED) {
            WaitingThread newWaiting = new WaitingThread(Thread.currentThread(), null, null);
            synchronized (this) {
                newWaiting.previous = waiting;
                waiting = newWaiting;
            }

            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (newWaiting) {
                if(newWaiting.state == T_NOT_ENQUEED) {
                    newWaiting.state = T_ENQUEED;
                }
                else {
                    // writer thread already asked us to take responsibility
                    // so let us go to return value
                    return value;
                }
            }

            while (state != S_INITIALIZED) {
                LockSupport.park();
                if (Thread.currentThread().isInterrupted())
                    throw new InterruptedException();
            }
        }

        return value;
    }

    /**
     * Assigns a value to the variable. Can only be invoked once on each instance of DataFlowVariable
     * @param value The value to assign
     */
    public final void bind(final T value) {
        synchronized (this) {
            if (state != S_NOT_INITIALIZED)
                throw new IllegalStateException("A DataFlowVariable can only be assigned once.");

            this.value = value;
            state = S_INITIALIZED;
        }

        // no more new waiting threads since that point
        WaitingThread w = waiting;
        waiting = null;
        for ( ; w != null; w = w.previous) {
            boolean unpark;
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (w) {
                if (w.state == T_ENQUEED) {
                    w.state = T_DEQUEED;
                    unpark = true;
                }
                else {
                    if (w.state == T_NOT_ENQUEED) {
                        w.state = T_DEQUEED;
                        // reader has to take care for itself
                        unpark = false;
                    }
                    else {
                        unpark = true;
                    }
                }
            }

            if (unpark)
                unparkOrCallback(w);
        }
    }

    private void unparkOrCallback(WaitingThread waiting) {
        if (waiting.thread != null)
            LockSupport.unpark(waiting.thread);
        else {
            scheduleCallback(waiting.callback);
        }
    }

    private void scheduleCallback(final DataCallback callback) {
        new DataFlowActor() {
            @Override
            protected void act() {
                callback.onData(value);
            }
        }.start();
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

    /**
     * Schedule closure to be executed by pooled actor after data became available
     * It is important to notice that even if data already available the execution of closure
     * will not happen immediately but will be scheduled 
     *
     * @param closure closure to execute when data available
     */
    public void rightShift (final Closure closure)  {
        whenBound(closure);
    }

    /**
     * Schedule closure to be executed by pooled actor after data became available
     * It is important to notice that even if data already available the execution of closure
     * will not happen immediately but will be scheduled
     *
     * @param closure closure to execute when data available
     */
    public void whenBound (final Closure closure)  {
        getVal(new DataCallback<T> () {
            public void onData(T data) {
                closure.call(data);
            }
        });
    }
}