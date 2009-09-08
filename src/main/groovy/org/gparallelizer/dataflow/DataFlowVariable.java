//  GParallelizer
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License. 

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
@SuppressWarnings({"AccessingNonPublicFieldOfAnotherObject", "UnqualifiedStaticUsage"})
public final class DataFlowVariable<T> {
    @SuppressWarnings({"InstanceVariableMayNotBeInitialized"})
    private volatile T value;

    private final AtomicInteger state = new AtomicInteger();

    private final AtomicReference<WaitingThread<T>> waiting = new AtomicReference<WaitingThread<T>> ();

    private static final int S_NOT_INITIALIZED = 0;
    private static final int S_INITIALIZING    = 1;
    private static final int S_INITIALIZED     = 2;

    private static class WaitingThread<V> {
        private final Thread thread;
        private volatile WaitingThread<V> previous;
        private final DataCallback<V> callback;

        private WaitingThread(final Thread thread, final WaitingThread<V> previous, final DataCallback<V> callback) {
            this.callback = callback;
            this.thread = thread;
            this.previous = previous;
        }
    }

    private static final WaitingThread dummyWaitingThread = new WaitingThread<Object>(null, null, null);

    private interface DataCallback<U> {
        void onData (U data);
    }

    public DataFlowVariable () {
        state.set(S_NOT_INITIALIZED);
    }

    public void getVal(final DataCallback<T> callback) {
        WaitingThread<T> newWaiting = null;
        while (state.get() != S_INITIALIZED) {
            if (newWaiting == null)
                newWaiting = new WaitingThread<T>(null, null, callback);

            final WaitingThread<T> previous = waiting.get();
            // it means that writer already started processing queue, so value is already in place
            if (previous == dummyWaitingThread)
                break;

            newWaiting.previous = previous;
            if (waiting.compareAndSet(previous, newWaiting)) {
                // ok, we are in the queue, so writer is responsible to process us
                return;
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
        WaitingThread<T> newWaiting = null;
        while (state.get() != S_INITIALIZED) {
            if (newWaiting == null)
                newWaiting = new WaitingThread<T>(Thread.currentThread(), null, null);

            final WaitingThread<T> previous = waiting.get();
            // it means that writer already started processing queue, so value is already in place
            if (previous == dummyWaitingThread)
                break;

            newWaiting.previous = previous;
            if (waiting.compareAndSet(previous, newWaiting)) {
                // ok, we are in the queue, so writer is responsible to process us
                while (state.get() != S_INITIALIZED) {
                    LockSupport.park();
                    if (Thread.currentThread().isInterrupted())
                        throw new InterruptedException();
                }
                break;
            }
        }

        return value;
    }

    /**
     * Assigns a value to the variable. Returns silently if invoked on an already bound variable.
     * @param value The value to assign
     */
    public void bindSafely(final T value) {
        if (!state.compareAndSet(S_NOT_INITIALIZED, S_INITIALIZING))
            return;
        doBind(value);
    }

    /**
     * Assigns a value to the variable. Can only be invoked once on each instance of DataFlowVariable
     * Throws exception if invoked on an already bound variable.
     * @param value The value to assign
     */
    public void bind(final T value) {
        if (!state.compareAndSet(S_NOT_INITIALIZED, S_INITIALIZING))
            throw new IllegalStateException("A DataFlowVariable can only be assigned once.");

        doBind(value);
    }

    private void doBind(final T value) {
        this.value = value;
        state.set(S_INITIALIZED);

        final WaitingThread<T> waitingQueue = waiting.getAndSet(dummyWaitingThread);
        // no more new waiting threads since that point

        // no more new waiting threads since that point
        for ( WaitingThread<T> waiting = waitingQueue; waiting != null; waiting = waiting.previous) {
            if (waiting.thread != null)
                LockSupport.unpark(waiting.thread);  //can be potentially called on a not parked thread
            else {
                scheduleCallback(waiting.callback);
            }
        }
    }

    private void scheduleCallback(final DataCallback<T> callback) {
        // TODO: we don't need actor here. Unfortunately DataFlowActor.DATA_FLOW_GROUP.getThreadPool is not acessible here
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

    @SuppressWarnings({"ArithmeticOnVolatileField"})
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
            public void onData(final T data) {
                closure.call(data);
            }
        });
    }
}
