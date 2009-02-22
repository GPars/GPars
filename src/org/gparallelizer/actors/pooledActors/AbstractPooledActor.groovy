package org.gparallelizer.actors.pooledActors

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.gparallelizer.actors.Actor
import org.gparallelizer.actors.pooledActors.ActorAction
import org.gparallelizer.actors.pooledActors.PooledActor
import static org.gparallelizer.actors.pooledActors.ActorAction.actorAction
import static org.gparallelizer.actors.pooledActors.ActorException.CONTINUE
import static org.gparallelizer.actors.pooledActors.ActorException.TERMINATE

/**
 * Created by IntelliJ IDEA.
 * User: vaclav
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
     * Internal lock to synchronize access of external threads calling send() or stop() with the current active action
     */
    private final Object lock = new Object()

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

    public final Actor stop() {
        synchronized (lock) {
            indicateStop()
            (codeReference.getAndSet(null)) ? actorAction(this) { throw TERMINATE } :  currentAction.get()?.cancel()
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

    protected final void react(final Closure code) {

        Closure reactCode = {message ->
            code.call(message)
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
            }
        }
        throw CONTINUE
    }

    public final Actor send(Object message) {
        synchronized (lock) {
            checkState()
            final Closure currentReference = codeReference.getAndSet(null)
            if (currentReference) {
                actorAction(this) { currentReference.call(message) }
            } else {
                messageQueue.put(message)
            }
        }
        return this
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

    /**
     * Joins the actor's thread
     * @param milis Timeout in miliseconds
     */
    void join(long milis) {
        //todo implement or refactor away from the PooledActor interface
        throw new UnsupportedOperationException("Pooled actors don't support join")
    }

    //todo timeout support
    //todo try standard thread pool
    //todo reorganize packages
    //todo document

    //todo replyTo support
    //todo handle nested loops
    //todo make codeReference non-atomic
}