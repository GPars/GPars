package org.gparallelizer.actors

import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import org.gparallelizer.actors.util.EnhancedSemaphore
import org.codehaus.groovy.runtime.TimeCategory
import groovy.time.Duration;

/**
 * Default Actor implementation designed to be extended with actual message queue and the act() method.
 * The actor must be started after construction  by calling the start() method. This will start the background
 * actor thread, which first calls an afterStart() method of the Actor, if the method exists, and then keeps
 * calling the act() method, until the stop() method is called or the actor thread is interrupted directly.
 * Before the actor thread finishes an beforeStop() method is called, if exists.
 * After it stops the afterStop(List unprocessedMessages) is called, if exists,
 * with all the unprocessed messages from the queue as a parameter.
 * The Actor can be restarted be calling start() again.
 * Each Actor can define lifecycle observing methods, which will be called by the Actor's background thread whenever a certain lifecycle event occurs.
 * <ul>
 * <li>afterStart() - called immediatelly after the Actor's background thread has been started, before the act() method is called the first time.</li>
 * <li>beforeStop() - called right before the actor stops.</li>
 * <li>afterStop(List undeliveredMessages) - called right after the actor is stopped, passing in all the messages from the queue.</li>
 * <li>onInterrupt(InterruptedException? e) - called when the actor's thread gets interrupted. Thread interruption will result in the stopping the actor in any case.</li>
 * <li>onException(Throwable e) - called when an exception occurs in the actor's thread. Throwing an exception from this method will stop the actor.</li>
 * </ul>
 *
 * @author Vaclav Pech
 * Date: Jan 7, 2009
 */
abstract public class AbstractActor implements ThreadedActor {
    /**
     * Queue for the messages
     */
    final BlockingQueue<ActorMessage> messageQueue;
    //todo should be private but the closure in doReceive() method would not see it

    /**
     * The actors background thread.
     */
    protected volatile Thread actorThread;

    /**
     * Flag indicating Actor's liveness status.
     */
    private final AtomicBoolean started = new AtomicBoolean(false);

    private final EnhancedSemaphore startupLock = new EnhancedSemaphore(1);

    //todo add generics
    /**
     * Creates a new Actor using the passed-in queue to store incoming messages.
     */
    public AbstractActor(final BlockingQueue<ActorMessage> messageQueue) {
        if (messageQueue == null) throw new IllegalArgumentException("Actor message queue must not be null.")
        this.messageQueue = messageQueue;
    }

    /**
     * Starts the Actor. No messages can be send or received before an Actor is started.
     */
    public final Actor start() {
        //todo should be inlined but currently it wouldn't be visible inside the closure if mixin is used
        def localStarted = started
        //todo should be inlined but currently it wouldn't be visible inside the closure if mixin is used
        def localStartupLock = startupLock

        localStartupLock.withSemaphore {
            if (localStarted.getAndSet(true)) throw new IllegalStateException("Actor already started")
        }
        actorThread = Thread.start(createThreadName()) {
            try {
                ReplyRegistry.registerCurrentActorWithThread this
                if (delegate.respondsTo('afterStart')) delegate.afterStart()
                while (!Thread.currentThread().interrupted()) {
                    try {
                        use(TimeCategory) {
                            act();
                        }
                    } catch (InterruptedException e) {
                        if (delegate.respondsTo('onInterrupt')) delegate.onInterrupt(e)
                        Thread.currentThread().interrupt()
                    } catch (Throwable e) {
                        reportError(delegate, e)
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace(System.err) //invoked when the onException handler threw an exception
            } finally {
                try {
                    if (delegate.respondsTo('beforeStop')) delegate.beforeStop()
                    localStartupLock.withSemaphore {
                        localStarted.set(false)
                        if (this.respondsTo('afterStop')) this.afterStop(sweepQueue())
                    }
                } catch (Throwable e) {
                    try {
                        reportError(delegate, e)
                    } catch (Throwable ex) {ex.printStackTrace(System.err)} //invoked when the onException handler threw an exception
                }
                ReplyRegistry.deregisterCurrentActorWithThread()
            }
        }
        return this
    }

    /**
     * Stops the Actor. The background thread will be stopped, unprocessed messages will be passed to the afterStop
     * method, if exists.
     * Has no effect if the Actor is not started.
     */
    public final Actor stop() {
        actorThread?.interrupt()
        return this
    }

    /**
     * Checks the current status of the Actor.
     */
    public final boolean isActive() {
        return started.get()
    }

    //todo should be private but mixins woudn't work
    /**
     * Does the actual message receive using the supplied closure and wraps it with all necessary ceremony
     */
    final Object doReceive(Closure code) throws InterruptedException {
        checkState();
        ActorMessage message = code()
        if (!message) return null
        ReplyEnhancer.enhanceWithReplyMethods(this, message)
        return message.payLoad;
    }

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     * @return The message retrieved from the queue.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    protected final Object receive() throws InterruptedException {
        doReceive {messageQueue.take()}
    }

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     * @param timeout how long to wait before giving up, in units of unit
     * @param timeUnit a TimeUnit determining how to interpret the timeout parameter
     * @return The message retrieved from the queue, or null, if the timeout expires.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    protected final Object receive(long timeout, TimeUnit timeUnit) throws InterruptedException {
        doReceive {messageQueue.poll(timeout, timeUnit)}
    }

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     * @param duration how long to wait before giving up, in units of unit
     * @return The message retrieved from the queue, or null, if the timeout expires.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    protected final Object receive(Duration duration) throws InterruptedException {
        return receive(duration.toMilliseconds(), TimeUnit.MILLISECONDS);
    }

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     * The message retrieved from the queue is passed into the handler as the only parameter.
     * @param handler A closure accepting the retrieved message as a parameter, which will be invoked after a message is received.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    protected final void receive(Closure handler) throws InterruptedException {
        int maxNumberOfParameters = handler.getMaximumNumberOfParameters()
        if (maxNumberOfParameters == 0) {
            receive()
            handler.call()
        } else {
            handler.call(*(1..maxNumberOfParameters).collect { receive() })
        }
    }

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     * The message retrieved from the queue is passed into the handler as the only parameter.
     * A null value is passed into the handler, if the timeout expires
     * @param timeout how long to wait before giving up, in units of unit
     * @param timeUnit a TimeUnit determining how to interpret the timeout parameter
     * @param handler A closure accepting the retrieved message as a parameter, which will be invoked after a message is received.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    protected final void receive(long timeout, TimeUnit timeUnit, Closure handler) throws InterruptedException {
        int maxNumberOfParameters = handler.getMaximumNumberOfParameters()
        if (maxNumberOfParameters == 0) {
            receive(timeout, timeUnit)
            handler.call()
        } else {
            long stopTime = timeUnit.toMillis(timeout) + System.currentTimeMillis()
            boolean nullAppeared = false  //Ignore further potential messages once a null is retrieved (due to a timeout)
            handler.call(*(1..maxNumberOfParameters).collect {
                if (nullAppeared) return null
                else {
                    Object message = receive(Math.max(stopTime - System.currentTimeMillis(), 0), TimeUnit.MILLISECONDS)
                    nullAppeared = (message == null)
                    return message
                }
            })
        }
    }

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     * The message retrieved from the queue is passed into the handler as the only parameter.
     * A null value is passed into the handler, if the timeout expires
     * @param duration how long to wait before giving up, in units of unit
     * @param handler A closure accepting the retrieved message as a parameter, which will be invoked after a message is received.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    protected final void receive(Duration duration, Closure handler) throws InterruptedException {
        handler.call(receive(duration.toMilliseconds(), TimeUnit.MILLISECONDS))
    }

    /**
     * Adds the message to the Actor's message queue.
     * The method will wait for space to become available in the queue, if it is full.
     * It can only be called on a started Actor.
     * @return The same Actor instance
     * @throws InterruptedException If the thread is interrupted during the wait.
     */
    public final Actor send(Object message) throws InterruptedException {
        checkState()
        messageQueue.put(ActorMessage.build(message))
        return this
    }

    /**
     * Adds the message to the Actor's message queue.
     * The method will wait for space to become available in the queue, if it is full.
     * It can only be called on a started Actor.
     * @return The same Actor instance
     * @throws InterruptedException If the thread is interrupted during the wait.
     */
    public final Actor leftShift(Object message) throws InterruptedException {
        send message
    }

    /**
     * This method is called periodically from the Actor's thread until the Actor is stopped
     * with a call to the stop() method or the background thread is interrupted.
     * Provides an extension point for subclasses to provide their custom Actor's message handling code.
     * The default implementation throws UnsupportedOperationException.
     */
    protected void act() {
        throw new UnsupportedOperationException("The act() method must be overriden")
    }

    /**
     * Returns the actor's thread
     */
    protected final Thread getActorThread() { actorThread }

    /**
     * Joins the actor's thread
     * @param milis Timeout in miliseconds
     */
    public final void join(long milis) {
        actorThread?.join(milis)
    }

    //todo should be private, but closures demand higher visibility
    void reportError(def delegate, Throwable e) {
        if (delegate.respondsTo('onException')) delegate.onException(e)
        else {
            System.err.println("An exception occured in the Actor thread ${Thread.currentThread().name}")
            e.printStackTrace(System.err)
        }
    }

    //todo should be private, but closures demand higher visibility
    /**
     * Clears the message queue returning all the messages it held.
     * @return The messages stored in the queue
     */
    final List sweepQueue() {
        def messages = []
        Object message = messageQueue.poll()
        while (message != null) {
            messages << message
            message = messageQueue.poll()
        }
        return messages
    }

    //todo should be private, but mixins would not work properly
    /**
     * Checks, whether the Actor is active.
     * @throws IllegalStateException If the Actor is not active.
     */
    void checkState() {
        if (!started.get()) throw new IllegalStateException("The actor hasn't been started.");
    }

    /**
     * Created a JVM-unique name for Actors' threads.
     */
    private final String createThreadName() {
        "Actor Thread ${threadCount.incrementAndGet()}"
    }

    /**
     * Unique counter for Actors' threads
     */
    private static final AtomicLong threadCount = new AtomicLong(0)


}