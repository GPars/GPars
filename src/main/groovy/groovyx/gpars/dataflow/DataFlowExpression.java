//  GPars (formerly GParallelizer)
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

package groovyx.gpars.dataflow;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import groovy.lang.MetaProperty;
import org.codehaus.groovy.runtime.InvokerHelper;
import groovyx.gpars.MessageStream;
import groovyx.gpars.remote.RemoteConnection;
import groovyx.gpars.remote.RemoteHost;
import groovyx.gpars.serial.SerialContext;
import groovyx.gpars.serial.SerialMsg;
import groovyx.gpars.serial.WithSerialId;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Alex Tkachman
 */
public abstract class DataFlowExpression<T> extends WithSerialId implements GroovyObject {

    protected static final AtomicIntegerFieldUpdater<DataFlowExpression> stateUpdater
            = AtomicIntegerFieldUpdater.newUpdater(DataFlowExpression.class, "state");

    protected static final AtomicReferenceFieldUpdater<DataFlowExpression, WaitingThread> waitingUpdater
            = AtomicReferenceFieldUpdater.newUpdater(DataFlowExpression.class, WaitingThread.class, "waiting");

    private MetaClass metaClass = InvokerHelper.getMetaClass(getClass());

    /**
     * Holds the actual value. Is null before a concrete value is bound to it.
     */
    @SuppressWarnings({"InstanceVariableMayNotBeInitialized"})
    protected volatile T value;

    /**
     * Holds the current state of the variable
     */
    protected volatile int state;

    /**
     * Points to the head of the chain of requests waiting for a value to be bound
     */
    private volatile WaitingThread<T> waiting;

    protected static final int S_NOT_INITIALIZED = 0;
    protected static final int S_INITIALIZING = 1;
    protected static final int S_INITIALIZED = 2;

    /**
     * A logical representation of a synchronous or asynchronous request to read the value once it is bound.
     *
     * @param <V> The type of the value to bind
     */
    private static class WaitingThread<V> extends AtomicBoolean {
        private final Thread thread;
        private volatile WaitingThread<V> previous;
        private final MessageStream callback;
        private final Object attachment;

        /**
         * Creates a representation of the request to read the value once it is bound
         *
         * @param thread     The physical thread of the request, which will be suspended
         * @param previous   The previous request in the chain of requests
         * @param attachment An arbitrary object closely identifying the request for the caller
         * @param callback   An actor or operator to send a message to once a value is bound
         */
        private WaitingThread(final Thread thread, final WaitingThread<V> previous, final Object attachment, final MessageStream callback) {
            this.callback = callback;
            this.attachment = attachment;
            this.thread = thread;
            this.previous = previous;
        }
    }

    /**
     * A request chain terminator
     */
    private static final WaitingThread dummyWaitingThread = new WaitingThread<Object>(null, null, null, null);

    /**
     * Creates a new unbound Dataflow Expression
     */
    public DataFlowExpression() {
        state = S_NOT_INITIALIZED;
    }

    /**
     * Asynchronously retrieves the value of the variable. Sends the actual value of the variable as a message
     * back the the supplied actor once the value has been bound.
     * The actor can perform other activities or release a thread back to the pool by calling react() waiting for the message
     * with the value of the Dataflow Variable.
     *
     * @param callback An actor to send the bound value to.
     */
    public void getValAsync(final MessageStream callback) {
        getValAsync(null, callback);
    }

    /**
     * Used by Dataflow operators.
     * Asynchronously retrieves the value of the variable. Sends a message back the the supplied actor
     * with a map holding the supplied index under the 'index' key and the actual value of the variable under
     * the 'result' key once the value has been bound.
     * Index is an arbitrary value helping the actor.operator match its request with the reply.
     * The actor/operator can perform other activities or release a thread back to the pool by calling react() waiting for the message
     * with the value of the Dataflow Variable.
     *
     * @param attachment arbitary non-null attachment if reader needs better identification of result
     * @param callback   An actor to send the bound value plus the supplied index to.
     */
    void getValAsync(final Object attachment, final MessageStream callback) {
        if (callback == null)
            throw new NullPointerException();

        WaitingThread<T> newWaiting = null;
        while (state != S_INITIALIZED) {
            if (newWaiting == null) {
                newWaiting = new WaitingThread<T>(null, null, attachment, callback);
            }

            final WaitingThread<T> previous = waiting;
            // it means that writer already started processing queue, so value is already in place
            if (previous == dummyWaitingThread) {
                break;
            }

            newWaiting.previous = previous;
            if (waitingUpdater.compareAndSet(this, previous, newWaiting)) {
                // ok, we are in the queue, so writer is responsible to process us
                return;
            }
        }

        scheduleCallback(attachment, callback);
    }

    /**
     * Reads the value of the variable. Blocks, if the value has not been assigned yet.
     *
     * @return The actual value
     * @throws InterruptedException If the current thread gets interrupted while waiting for the variable to be bound
     */
    public T getVal() throws InterruptedException {
        WaitingThread<T> newWaiting = null;
        while (state != S_INITIALIZED) {
            if (newWaiting == null) {
                newWaiting = new WaitingThread<T>(Thread.currentThread(), null, null, null);
            }

            final WaitingThread<T> previous = waiting;
            // it means that writer already started processing queue, so value is already in place
            if (previous == dummyWaitingThread) {
                break;
            }

            newWaiting.previous = previous;
            if (waitingUpdater.compareAndSet(this, previous, newWaiting)) {
                // ok, we are in the queue, so writer is responsible to process us
                while (state != S_INITIALIZED) {
                    LockSupport.park();
                    if (Thread.currentThread().isInterrupted()) {
                        newWaiting.set(true); // don't unpark please
                        throw new InterruptedException();
                    }
                }
                break;
            }
        }

        return value;
    }

    /**
     * Reads the value of the variable. Blocks up to given timeout, if the value has not been assigned yet.
     *
     * @return The actual value
     * @throws InterruptedException If the current thread gets interrupted while waiting for the variable to be bound
     */
    public T getVal(final long timeout, final TimeUnit units) throws InterruptedException {
        final long endNano = System.nanoTime() + units.toNanos(timeout);
        WaitingThread<T> newWaiting = null;
        while (state != S_INITIALIZED) {
            if (newWaiting == null) {
                newWaiting = new WaitingThread<T>(Thread.currentThread(), null, null, null);
            }

            final WaitingThread<T> previous = waiting;
            // it means that writer already started processing queue, so value is already in place
            if (previous == dummyWaitingThread) {
                break;
            }

            newWaiting.previous = previous;
            if (waitingUpdater.compareAndSet(this, previous, newWaiting)) {
                // ok, we are in the queue, so writer is responsible to process us
                while (state != S_INITIALIZED) {
                    final long toWait = endNano - System.nanoTime();
                    if (toWait <= 0) {
                        newWaiting.set(true); // don't unpark please
                        return null;
                    }

                    LockSupport.parkNanos(toWait);
                    if (Thread.currentThread().isInterrupted()) {
                        newWaiting.set(true); // don't unpark please
                        throw new InterruptedException();
                    }
                }
                break;
            }
        }

        return value;
    }

    /**
     * Assigns a value to the variable. Returns silently if invoked on an already bound variable.
     *
     * @param value The value to assign
     */
    public void bindSafely(final T value) {
        if (!stateUpdater.compareAndSet(this, S_NOT_INITIALIZED, S_INITIALIZING)) {
            return;
        }
        doBind(value);
    }

    /**
     * Assigns a value to the variable. Can only be invoked once on each instance of DataFlowVariable
     * Throws exception if invoked on an already bound variable.
     *
     * @param value The value to assign
     */
    public void bind(final T value) {
        if (!stateUpdater.compareAndSet(this, S_NOT_INITIALIZED, S_INITIALIZING)) {
            throw new IllegalStateException("A DataFlowVariable can only be assigned once.");
        }

        doBind(value);
    }

    /**
     * Performs the actual bind operation, unblocks all blocked threads and informs all asynchronously waiting actors.
     *
     * @param value The value to assign
     */
    private void doBind(final T value) {
        doBindImpl(value);
        notifyRemote(null);
    }

    private void doBindImpl(final T value) {
        this.value = value;
        state = S_INITIALIZED;

        @SuppressWarnings({"unchecked"})
        final WaitingThread<T> waitingQueue = waitingUpdater.getAndSet(this, dummyWaitingThread);

        // no more new waiting threads since that point
        for (WaitingThread<T> currentWaiting = waitingQueue; currentWaiting != null; currentWaiting = currentWaiting.previous) {
            // maybe currentWaiting thread canceled or was interrupted
            if (currentWaiting.compareAndSet(false, true)) {
                if (currentWaiting.thread != null) {
                    // can be potentially called on a non-parked thread,
                    // which is OK as in this case next park () will be ignored
                    LockSupport.unpark(currentWaiting.thread);
                } else {
                    if (currentWaiting.callback != null)
                        scheduleCallback(currentWaiting.attachment, currentWaiting.callback);
                }
            }
        }
    }

    public void doBindRemote(final BindDataFlow msg) {
        doBindImpl(value);
        notifyRemote(msg.hostId);
    }

    private void notifyRemote(final UUID hostId) {
        if (serialHandle != null)
            DataFlowActor.DATA_FLOW_GROUP.getThreadPool().execute(new Runnable() {
                public void run() {
                    Object sub = serialHandle.getSubscribers();
                    if (sub instanceof SerialContext) {
                        RemoteHost host = (RemoteHost) sub;
                        if (hostId == null || !host.getHostId().equals(hostId)) {
                            host.write(new BindDataFlow(DataFlowExpression.this, value, host.getLocalHost().getId()));
                        }
                    }

                    if (sub instanceof List) {
                        //noinspection SynchronizeOnNonFinalField
                        synchronized (serialHandle) {
                            //noinspection unchecked
                            for (SerialContext host : (List<SerialContext>) sub) {
                                if (hostId == null || !host.getHostId().equals(hostId)) {
                                    host.write(new BindDataFlow(DataFlowExpression.this, value, host.getLocalHostId()));
                                }
                            }
                        }
                    }
                }
            });
    }

    /**
     * Sends the result back to the actor, which is waiting asynchronously for the value to be bound.
     * The message will either be a map holding the index under the 'index' key and the actual bound value under the 'result' key,
     * or it will be the result itself if the callback doesn't care about the index.
     *
     * @param attachment
     * @param callback   The actor to send the message to
     */
    @SuppressWarnings({"TypeMayBeWeakened"})
    private void scheduleCallback(final Object attachment, final MessageStream callback) {
        if (attachment == null) {
            callback.send(value);
        } else {
            final Map<String, Object> message = new HashMap<String, Object>();
            message.put("attachment", attachment);
            message.put("result", value);
            callback.send(message);
        }
    }

    /**
     * Schedule closure to be executed by pooled actor after data became available
     * It is important to notice that even if data already available the execution of closure
     * will not happen immediately but will be scheduled
     *
     * @param closure closure to execute when data available
     */
    public void rightShift(final Closure closure) {
        whenBound(closure);
    }

    /**
     * Schedule closure to be executed by pooled actor after data became available
     * It is important to notice that even if data already available the execution of closure
     * will not happen immediately but will be scheduled.
     *
     * @param closure closure to execute when data available
     */
    public void whenBound(final Closure closure) {
        getValAsync(new DataCallback(closure));
    }

    /**
     * Send result to provided stream when became available
     *
     * @param stream stream where to send result
     */
    public void whenBound(MessageStream stream) {
        getValAsync(stream);
    }

    @SuppressWarnings("unchecked")
    public static <V> DataFlowExpression<V> transform(final Object another, final Closure closure) {
        int pnum = closure.getMaximumNumberOfParameters();
        if (pnum == 0) {
            throw new IllegalArgumentException("Closure should have parameters");
        }

        if (pnum == 1) {
            return new TransformOne<V>(another, closure);
        } else {
            if (another instanceof Collection) {
                Collection collection = (Collection) another;
                if (collection.size() != pnum)
                    throw new IllegalArgumentException("Closure parameters don't match #of arguments");

                return new TransformMany<V>(collection, closure);
            }

            throw new IllegalArgumentException("Collection expected");
        }
    }

    /**
     * Schedule closure to be executed by pooled actor after data became available
     * It is important to notice that even if data already available the execution of closure
     * will not happen immediately but will be scheduled.
     * Used by the DataFlowStream class, since it cannot pass closures directly.
     *
     * @param closure An object with a method matching the 'perform(Object value)' signature
     */
    void whenBound(final Object closure) {
        getValAsync(new DataCallback(closure));
    }

    /**
     * Utility method to call at the very end of constructor of derived expressions.
     * Create and subscribe listener
     */
    protected final void subscribe() {
        DataFlowExpressionsCollector listener = new DataFlowExpressionsCollector();
        subscribe(listener);
        listener.start();
    }

    /**
     * Evaluate expression after the ones we depend from are ready
     *
     * @return value to bind
     */
    protected T evaluate() {
        return value;
    }

    protected void subscribe(DataFlowExpressionsCollector listener) {
        listener.subscribe(this);
    }

    public Object invokeMethod(String name, Object args) {
        if (getMetaClass().respondsTo(this, name).isEmpty()) {
            return new DataFlowInvocationExpression(this, name, (Object[]) args);
        }
        return InvokerHelper.invokeMethod(this, name, args);

    }

    /**
     * Returns either standard property of expression or
     * creates expression, which will request given property when receiver became available
     *
     * @param propertyName The name of the property to retrieve
     * @return The property value, instance of DataFlowGetPropertyExpression 
     */
    public Object getProperty(String propertyName) {
        MetaProperty metaProperty = getMetaClass().hasProperty(this, propertyName);
        if (metaProperty != null)
            return metaProperty.getProperty(this);

        return new DataFlowGetPropertyExpression(this, propertyName);
    }

    public void setMetaClass(MetaClass metaClass) {
        this.metaClass = metaClass;
    }

    public void setProperty(String propertyName, Object newValue) {
        metaClass.setProperty(this, propertyName, newValue);
    }

    public MetaClass getMetaClass() {
        return metaClass;
    }

    /**
     * Listener for availability of data flow expressions we depend from
     */
    protected final class DataFlowExpressionsCollector extends MessageStream {
        private final AtomicInteger count = new AtomicInteger(1);

        public DataFlowExpressionsCollector() {
        }

        public MessageStream send(Object message) {
            if (count.decrementAndGet() == 0) {
                bind(evaluate());
            }
            return this;
        }

        protected final Object subscribe(Object element) {
            if (!(element instanceof DataFlowExpression)) {
                return element;
            }

            DataFlowExpression dataFlowExpression = (DataFlowExpression) element;
            if (dataFlowExpression.state == S_INITIALIZED) {
                return dataFlowExpression.value;
            }

            count.incrementAndGet();
            dataFlowExpression.getValAsync(this);
            return element;
        }


        protected void start() {
            if (count.decrementAndGet() == 0) {
                doBind(evaluate());
            }
        }
    }

    @SuppressWarnings({"ArithmeticOnVolatileField"})
    @Override
    public String toString() {
        return getClass().getSimpleName() + "(value=" + value + ')';
    }

    /**
     * Never use this method.
     */
    public void rebind() {
        state = S_NOT_INITIALIZED;
    }

    private static class TransformOne<V> extends DataFlowExpression<V> {
        Object arg;
        private final Closure closure;

        public TransformOne(Object another, Closure closure) {
            this.closure = closure;
            arg = another;
        }

        protected V evaluate() {
            //noinspection unchecked
            return (V) closure.call(arg instanceof DataFlowExpression ? ((DataFlowExpression) arg).value : arg);
        }

        protected void subscribe(DataFlowExpressionsCollector listener) {
            arg = listener.subscribe(arg);
        }
    }

    private static class TransformMany<V> extends DataFlowComplexExpression<V> {
        private final Closure closure;

        public TransformMany(Collection collection, Closure closure) {
            super(collection.toArray());
            this.closure = closure;
            subscribe();
        }

        protected V evaluate() {
            super.evaluate();
            //noinspection unchecked
            return (V) closure.call(args);
        }
    }

    public static class BindDataFlow extends SerialMsg {
        private DataFlowExpression var;
        private Object message;

        public BindDataFlow(DataFlowExpression var, Object message, UUID hostId) {
            this.var = var;
            this.message = message;
        }

        @Override
        public void execute(RemoteConnection conn) {
            var.doBindRemote(this);
        }
    }
}
