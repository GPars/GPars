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
import groovyx.gpars.actor.Actors;
import groovyx.gpars.actor.impl.MessageStream;
import groovyx.gpars.remote.RemoteConnection;
import groovyx.gpars.remote.RemoteHost;
import groovyx.gpars.serial.SerialContext;
import groovyx.gpars.serial.SerialMsg;
import groovyx.gpars.serial.WithSerialId;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;

/**
 * The base class for all dataflow elements.
 *
 * @author Alex Tkachman, Vaclav Pech
 */
public abstract class DataFlowExpression<T> extends WithSerialId implements GroovyObject {

    /**
     * Updater for the state field
     */
    protected static final AtomicIntegerFieldUpdater<DataFlowExpression> stateUpdater
            = AtomicIntegerFieldUpdater.newUpdater(DataFlowExpression.class, "state");

    /**
     * Updater for the waiting field
     */
    protected static final AtomicReferenceFieldUpdater<DataFlowExpression, WaitingThread> waitingUpdater
            = AtomicReferenceFieldUpdater.newUpdater(DataFlowExpression.class, WaitingThread.class, "waiting");

    /**
     * The current metaclass
     */
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

    /**
     * Possible states
     */
    protected static final int S_NOT_INITIALIZED = 0;
    protected static final int S_INITIALIZING = 1;
    protected static final int S_INITIALIZED = 2;

    /**
     * A logical representation of a synchronous or asynchronous request to read the value once it is bound.
     *
     * @param <V> The type of the value to bind
     */
    private static class WaitingThread<V> extends AtomicBoolean {
        private static final long serialVersionUID = 8909974768784947460L;
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
    protected DataFlowExpression() {
        state = S_NOT_INITIALIZED;
    }

    /**
     * Check if value has been set already for this expression
     *
     * @return true if bound already
     */
    public boolean isBound () {
       return state == S_INITIALIZED;
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
     * Asynchronously retrieves the value of the variable. Sends a message back the the supplied MessageStream
     * with a map holding the supplied attachment under the 'attachment' key and the actual value of the variable under
     * the 'result' key once the value has been bound.
     * Attachment is an arbitrary value helping the actor.operator match its request with the reply.
     * The actor/operator can perform other activities or release a thread back to the pool by calling react() waiting for the message
     * with the value of the Dataflow Variable.
     *
     * @param attachment arbitrary non-null attachment if reader needs better identification of result
     * @param callback   An actor to send the bound value plus the supplied index to.
     */
    void getValAsync(final Object attachment, final MessageStream callback) {
        if (callback == null) {
            throw new NullPointerException();
        }

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
     * @param timeout The timeout value
     * @param units Units for the timeout
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
                    if (currentWaiting.callback != null) {
                        scheduleCallback(currentWaiting.attachment, currentWaiting.callback);
                    }
                }
            }
        }
    }

    /**
     * Binds the value after receiving a bing message over the wire
     * @param hostId Id of the bind originator host
     * @param message The value to bind
     */
    public void doBindRemote(final UUID hostId, final T message) {
        doBindImpl(message);
        notifyRemote(hostId);
    }

    /**
     * Sends notifications to all subscribers
     * @param hostId The local host id
     */
    private void notifyRemote(final UUID hostId) {
        if (serialHandle != null) {
            Actors.defaultPooledActorGroup.getThreadPool().execute(new Runnable() {
                public void run() {
                    final Object sub = serialHandle.getSubscribers();
                    if (sub instanceof RemoteHost) {
                        final RemoteHost host = (RemoteHost) sub;
                        if (hostId == null || !host.getHostId().equals(hostId)) {
                            host.write(new BindDataFlow(DataFlowExpression.this, value, host.getLocalHost().getId()));
                        }
                    }

                    if (sub instanceof List) {
                        //noinspection SynchronizeOnNonFinalField
                        synchronized (serialHandle) {
                            //noinspection unchecked
                            for (final SerialContext host : (List<SerialContext>) sub) {
                                if (hostId == null || !host.getHostId().equals(hostId)) {
                                    host.write(new BindDataFlow(DataFlowExpression.this, value, host.getLocalHostId()));
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    /**
     * Sends the result back to the actor, which is waiting asynchronously for the value to be bound.
     * The message will either be a map holding the attachment under the 'attachment' key and the actual bound value under the 'result' key,
     * or it will be the result itself if the callback doesn't care about the index.
     *
     * @param attachment An arbitrary object identifying the request
     * @param callback   The actor to send the message to
     */
    @SuppressWarnings({"TypeMayBeWeakened"})
    private void scheduleCallback(final Object attachment, final MessageStream callback) {
        if (attachment == null) {
            callback.send(value);
        } else {
            final Map<String, Object> message = new HashMap<String, Object>(2);
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
     * Send result to provided stream when it becomes available
     *
     * @param stream stream where to send result
     */
    public void whenBound(final MessageStream stream) {
        getValAsync(stream);
    }

    @SuppressWarnings("unchecked")
    public static <V> DataFlowExpression<V> transform(final Object another, final Closure closure) {
        final int pnum = closure.getMaximumNumberOfParameters();
        if (pnum == 0) {
            throw new IllegalArgumentException("Closure should have parameters");
        }

        if (pnum == 1) {
            return new TransformOne<V>(another, closure);
        } else {
            if (another instanceof Collection) {
                final Collection collection = (Collection) another;
                if (collection.size() != pnum) {
                    throw new IllegalArgumentException("Closure parameters don't match #of arguments");
                }

                return new TransformMany<V>(collection, closure);
            }

            throw new IllegalArgumentException("Collection expected");
        }
    }

    /**
     * Utility method to call at the very end of constructor of derived expressions.
     * Create and subscribe listener
     */
    protected final void subscribe() {
        final DataFlowExpressionsCollector listener = new DataFlowExpressionsCollector();
        subscribe(listener);
        listener.start();
    }

    /**
     * Evaluate expression after the ones we depend on are ready
     *
     * @return value to bind
     */
    protected T evaluate() {
        return value;
    }

    protected void subscribe(final DataFlowExpressionsCollector listener) {
        listener.subscribe(this);
    }

    public Object invokeMethod(final String name, final Object args) {
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
    public Object getProperty(final String propertyName) {
        final MetaProperty metaProperty = getMetaClass().hasProperty(this, propertyName);
        if (metaProperty != null) {
            return metaProperty.getProperty(this);
        }

        return new DataFlowGetPropertyExpression(this, propertyName);
    }

    public void setMetaClass(final MetaClass metaClass) {
        this.metaClass = metaClass;
    }

    public void setProperty(final String propertyName, final Object newValue) {
        metaClass.setProperty(this, propertyName, newValue);
    }

    public MetaClass getMetaClass() {
        return metaClass;
    }

    /**
     * Listener for availability of data flow expressions we depend from
     */
    final class DataFlowExpressionsCollector extends MessageStream {
        private static final long serialVersionUID = 3414942165521113575L;
        private final AtomicInteger count = new AtomicInteger(1);

        @Override public MessageStream send(final Object message) {
            if (count.decrementAndGet() == 0) {
                bind(evaluate());
            }
            return this;
        }

        Object subscribe(final Object element) {
            if (!(element instanceof DataFlowExpression)) {
                return element;
            }

            final DataFlowExpression dataFlowExpression = (DataFlowExpression) element;
            if (dataFlowExpression.state == S_INITIALIZED) {
                return dataFlowExpression.value;
            }

            count.incrementAndGet();
            dataFlowExpression.getValAsync(this);
            return element;
        }


        void start() {
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

    private static class TransformOne<V> extends DataFlowExpression<V> {
        private static final long serialVersionUID = 6701886501249351047L;
        Object arg;
        private final Closure closure;

        private TransformOne(final Object another, final Closure closure) {
            this.closure = closure;
            arg = another;
        }

        @Override protected V evaluate() {
            //noinspection unchecked
            return (V) closure.call(arg instanceof DataFlowExpression ? ((DataFlowExpression) arg).value : arg);
        }

        @Override protected void subscribe(final DataFlowExpressionsCollector listener) {
            arg = listener.subscribe(arg);
        }
    }

    private static class TransformMany<V> extends DataFlowComplexExpression<V> {
        private static final long serialVersionUID = 4115456542358280855L;
        private final Closure closure;

        private TransformMany(final Collection collection, final Closure closure) {
            super(collection.toArray());
            this.closure = closure;
            subscribe();
        }

        @Override protected V evaluate() {
            super.evaluate();
            //noinspection unchecked
            return (V) closure.call(args);
        }
    }

    //todo sort out generics
    /**
     * Represents a remote message binding a value to a remoted DataFlowExpression
     */
    public static class BindDataFlow extends SerialMsg {
        private static final long serialVersionUID = -8674023870562062769L;
        private final DataFlowExpression expr;
        private final Object message;

        /**
         *
         * @param expr The local DataFlowExpression instance
         * @param message The actual value to bind
         * @param hostId The identification of the host to send the bind information to
         */
        public BindDataFlow(final DataFlowExpression expr, final Object message, final UUID hostId) {
            super(hostId);
            this.expr = expr;
            this.message = message;
        }

        /**
         * Performs the actual bind on the remote host
         *
         * @param conn The connection object
         */
        @Override
        public void execute(final RemoteConnection conn) {
            expr.doBindRemote(hostId, message);
        }
    }
}
