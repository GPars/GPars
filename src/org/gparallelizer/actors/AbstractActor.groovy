package org.gparallelizer.actors

import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import org.gparallelizer.actors.util.EnhancedSemaphore;

/**
 * Default Actor implementation designed to be extended with actual message queue and the act() method.
 * The actor must be started after construction  by calling the start() method. This will start the background
 * actor thread, which first calls an afterStart() method of the Actor, if the method exists, and then keeps
 * calling the act() method, until the stop() method is called or the actor thread is interrupted directly.
 * Before the actor thread finishes an beforeStop() method is called, if exists.
 * After it stops the afterStop(List unprocessedMessages) is called, if exists,
 * with all the unprocessed messages from the queue as a parameter.
 * The Actor can be restarted be calling start() again.
 *
 * @author Vaclav Pech
 * Date: Jan 7, 2009
 */

abstract public class AbstractActor implements Actor {
    /**
     * Queue for the messages
     */
    private final BlockingQueue messageQueue;

    /**
     * The actors background thread.
     */
    protected volatile Thread actorThread;

    //todo should be private but currently it wouldn't be visible inside closures
    /**
     * Flag indicating Actor's liveness status.
     */
    protected final AtomicBoolean started = new AtomicBoolean(false);

    //todo should be private but currently it wouldn't be visible inside closures
    protected final EnhancedSemaphore startupLock = new EnhancedSemaphore(1);

    //todo add generics
    /**
     * Creates a new Actor using the passed-in queue to store incoming messages.
     */
    public AbstractActor(final BlockingQueue messageQueue) {
        if (messageQueue == null) throw new IllegalArgumentException("Actor message queue must not be null.")
        this.messageQueue = messageQueue;
    }

    /**
     * Starts the Actor. No messages can be send or received before an Actor is started.
     */
    public final Actor start() {
        //todo should be inlined but currently it wouldn't be visible inside the closure if mixin is used
        def localStarted = started
        startupLock.withSemaphore {
            if (localStarted.getAndSet(true)) throw new IllegalStateException("Actor already started")
        }
        actorThread = Thread.start(createThreadName()) {
            try {
                if (delegate.respondsTo('afterStart')) delegate.afterStart()
                while (!Thread.currentThread().interrupted()) {
                    try {
                        act();
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
                    startupLock.withSemaphore {
                        started.set(false)
                        if (this.respondsTo('afterStop')) this.afterStop(sweepQueue())
                    }
                } catch (Throwable e) {
                    try {
                        reportError(delegate, e)
                    } catch (Throwable ex) {ex.printStackTrace(System.err)} //invoked when the onException handler threw an exception
                }
            }
        }
        return this
    }

    /**
     * Stops an Actor. The background thread will be stopped, unprocessed messages will be lost.
     * Has no effect if the Actor is not started.
     */
    public final Actor stop() {
        actorThread?.interrupt()
        return this
    }

    /**
     * Checks the current status of the Actor.
     */
    public boolean isActive() {
        return started.get()
    }

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     * @return The message retrieved from the queue.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    protected final Object receive() throws InterruptedException {
        checkState();
        return messageQueue.take();
    }

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     * @param how long to wait before giving up, in units of unit
     * @unit a TimeUnit determining how to interpret the timeout parameter
     * @return The message retrieved from the queue, or null, if the timeout expires.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    protected final Object receive(long timeout, TimeUnit timeUnit) throws InterruptedException {
        checkState();
        return messageQueue.poll(timeout, timeUnit);
    }

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     * The message retrieved from the queue is passed into the handler as the only parameter.
     * @param handler A closure accepting the retrieved message as a parameter, which will be invoked after a message is received.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    protected final void receive(Closure handler) throws InterruptedException {
        handler.call(receive())
    }

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     * The message retrieved from the queue is passed into the handler as the only parameter.
     * A null value is passed into the handler, if the timeout expires
     * @param how long to wait before giving up, in units of unit
     * @unit a TimeUnit determining how to interpret the timeout parameter
     * @param handler A closure accepting the retrieved message as a parameter, which will be invoked after a message is received.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    protected final void receive(long timeout, TimeUnit timeUnit, Closure handler) throws InterruptedException {
        handler.call(receive(timeout, timeUnit))
    }

    /**
     * Adds the message to the Actor's message queue.
     * The method will wait for space to become available in the queue, if it is full.
     * It can only be called on a started Actor.
     * @return The same Actor instance
     * @throws InterruptedException If the thread is interrupted during the wait.
     */
    public final Actor send(Object message) throws InterruptedException {
        checkState();
        messageQueue.put(message);
        return this
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
     * Joins the actor's thread
     * @param milis Timeout in miliseconds
     */
    void join(long milis) {
        actorThread?.join(milis)
    }

    //todo should be private, but mixins need higher visibility
    protected def reportError(def delegate, Throwable e) {
        if (delegate.respondsTo('onException')) delegate.onException(e)
        else {
            System.err.println("An exception occured in the Actor thread ${Thread.currentThread().name}")
            e.printStackTrace(System.err)
        }
    }

    //todo should be private, but mixins need higher visibility
    /**
     * Clears the message queue returning all the messages it held.
     * @return The messages stored in the queue
     */
    protected List sweepQueue() {
        def messages = []
        Object message = messageQueue.poll()
        while (message != null) {
            messages << message
            message = messageQueue.poll()
        }
        return messages
    }

    //todo should be private, but mixins need higher visibility
    /**
     * Checks, whether the Actor is active.
     * @throws IllegalStateException If the Actor is not active.
     */
    void checkState() {
        if (!started.get()) throw new IllegalStateException("The actor hasn't been started.");
    }

    //todo should be private, but mixins need higher visibility
    /**
     * Created a JVM-unique name for Actors' threads.
     */
    final String createThreadName() {
        "Actor Thread ${threadCount.incrementAndGet()}"
    }

    /**
     * Unique counter for Actors' threads
     */
    private static final AtomicLong threadCount = new AtomicLong(0)


}