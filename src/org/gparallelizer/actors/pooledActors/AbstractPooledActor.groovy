package org.gparallelizer.actors.pooledActors

import groovy.time.Duration
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.gparallelizer.actors.Actor
import org.gparallelizer.actors.pooledActors.ActorAction
import org.gparallelizer.actors.pooledActors.PooledActor
import static org.gparallelizer.actors.pooledActors.ActorAction.actorAction
import static org.gparallelizer.actors.pooledActors.ActorException.*

/**
 *
 * @author Vaclav Pech
 * Date: Feb 7, 2009
 */
abstract public class AbstractPooledActor implements PooledActor {

    /**
     * Queue for the messages
     */
    private final BlockingQueue messageQueue = new LinkedBlockingQueue();

    /**
     * Code for the next action
     */
    private final AtomicReference<Closure> codeReference = new AtomicReference<Closure>(null)

    /**
     * Indicates whether the actor should terminate
     */
    private final AtomicBoolean stopFlag = new AtomicBoolean(false)

    /**
     * Code for the loop, if any
     */
    private final AtomicReference<Closure> loopCode = new AtomicReference<Closure>(null)

    /**
     * The current active action (continuation) associated with the actor. An action must not use Actor's state
     * after it spounds a new action, only throw CONTINUE.
     */
    final AtomicReference<ActorAction> currentAction = new AtomicReference<ActorAction>(null)

    /**
     * The current timeout task, which will send a TIMEOUT message to the actor if not cancelled by someone
     * calling the send() method within the timeout specified for the currently blocked react() method.
     */
    private AtomicReference<TimerTask> timerTask = new AtomicReference<TimerTask>(null)

    /**
     * Timer holding timeouts for react methods
     */
    private Timer timer = new Timer()

    /**
     * Internal lock to synchronize access of external threads calling send() or stop() with the current active action
     */
    private final Object lock = new Object()

    //todo document
    static ThreadLocal<PooledActor> currentActor = new ThreadLocal<PooledActor>()

    /**
     * Starts the Actor. No messages can be send or received before an Actor is started.
     */
    public final AbstractPooledActor start() {
        actorAction(this) {
            if (delegate.respondsTo('afterStart')) delegate.afterStart()
            act()
        }
        return this
    }

    final void indicateStop() {
        stopFlag.set(true)
    }

    /**
     * Stops an Actor. The background thread will be stopped, unprocessed messages will be lost.
     * Has no effect if the Actor is not started.
     */
    public final Actor stop() {
        synchronized (lock) {
            indicateStop()
            (codeReference.getAndSet(null)) ? actorAction(this) { throw TERMINATE } : currentAction.get()?.cancel()
        }
        return this
    }

    /**
     * Checks the current status of the Actor.
     */
    public final boolean isActive() {
        return !stopFlag.get()
    }

    protected void act() {
        throw new UnsupportedOperationException("The act() method must be overriden")
    }

    protected final void react(final Duration duration, final Closure code) {
        react(duration.toMilliseconds(), code)
    }

    protected final void react(final Closure code) {
        react(0, code)
    }

    protected final void react(final long timeout, final Closure code) {

        Closure reactCode = {ActorMessage message ->
            if (message.payLoad == TIMEOUT) throw TIMEOUT
            
            this.metaClass.reply = {
                final PooledActor sender = message.sender
                if (sender) {
                    sender.send it
                } else {
                    throw new IllegalArgumentException("Cannot send a message ${it} to a null recipient.")
                }
            }

            code.call(message.payLoad)

            this.repeatLoop()
        }

        synchronized (lock) {
            if (stopFlag.get()) throw TERMINATE
            assert (codeReference.get() == null), "Cannot have more react called at the same time."

            final Object currentMessage = messageQueue.poll()
            if (currentMessage) {
                actorAction(this) { reactCode.call(currentMessage) }
            } else {
                codeReference.set(reactCode)
                if (timeout > 0) {
                    timerTask.set ([run:{ this.send(TIMEOUT) }] as TimerTask)
                    timer.schedule(timerTask.get(), timeout)
                }
            }
        }
        throw CONTINUE
    }

    /**
     * Adds a message to the Actor's queue. Can only be called on a started Actor.
     */
    public final Actor send(Object message) {
        synchronized (lock) {
            checkState()
            cancelCurrentTimeoutTimer(message)

            def actorMessage = new ActorMessage(message, currentActor.get())

            final Closure currentReference = codeReference.getAndSet(null)
            if (currentReference) {
                actorAction(this) { currentReference.call(actorMessage) }
            } else {
                messageQueue.put(actorMessage)
            }
        }
        return this
    }

    private def cancelCurrentTimeoutTimer(message) {
        if (message != TIMEOUT) timerTask.get()?.cancel()
    }

    /**
     * Checks, whether the Actor is active.
     * @throws IllegalStateException If the Actor is not active.
     */
    private void checkState() {
        if (stopFlag.get()) throw new IllegalStateException("The actor hasn't been started.");
    }

    protected final void repeatLoop() {
        final Closure code = loopCode.get()
        if (!code) return;
        doCall(code)
    }

    protected final void loop(final Closure code) {
        assert loopCode.get() == null, "The loop method must be only called once"
        loopCode.set(code)
        doCall(code)
    }

    private def doCall(Closure code) {
        if (stopFlag.get()) throw TERMINATE
        actorAction(this, code)
        throw CONTINUE
    }

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

    //todo try standard thread pool
    //todo document

    //todo retry after timeout or exception
    //todo support mixins
    //todo read system properties to configure pool
    //todo implement reply for thread-bound actors and between the two actor categories
    //todo replyIfExists support
    //todo handle nested loops
    //todo make codeReference non-atomic
}