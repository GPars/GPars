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
 * AbstractPooledActor provides the default PooledActor implementation. It represents a standalone active object (actor),
 * which reacts asynchronously to messages sent to it from outside through the send() method.
 * Each PooledActor has its own message queue and a thread pool shared with other PooledActors.
 * The work performed by a PooledActor is divided into chunks, which are sequentially submitted as independent tasks
 * to the thread pool for processing.
 * Whenever a PooledActor looks for a new message through the react() method, the actor gets detached
 * from the thread, making the thread available for other actors. Thanks to the ability to dynamically attach and detach
 * threads to actors, PooledActors can scale far beyond the limits of the underlying platform on number of cuncurrently
 * available threads.
 * The loop() method allows repeatedly invoke a closure and yet perform each of the iterations sequentially
 * in different thread from the thread pool.
 * To suport continuations correctly the react() and loop() methods never return.
 * <pre>
 * import static org.gparallelizer.actors.pooledActors.PooledActors.*
 *
 * def actor = actor {
 *     loop {
 *         react {message ->
 *             println message
 *         }
 *         //this line will never be reached
 *     }
 *     //this line will never be reached
 * }.start()
 *
 * actor.send 'Hi!'
 * </pre>
 * This requires the code to be structured accordingly.
 *
 * <pre>
 * def adder = actor {
 *     loop {
 *         react {a ->
 *             react {b ->
 *                 println a+b
 *                 replyIfExists a+b  //sends reply, if b was sent by a PooledActor
 *             }
 *         }
 *         //this line will never be reached
 *     }
 *     //this line will never be reached
 * }.start()
 * </pre>
 * The closures passed to the react() method can call reply() or replyIfExists(), which will send a message back to
 * the originator of the currently processed message. The replyIfExists() method unlike the reply() method will not fail
 * if the original message wasn't sent by an actor nor if the original sender actor is no longer running.
 * The react() method accepts timout specified using the TimeCategory DSL.
 * <pre>
 * react(10.MINUTES) {
 *     println 'Received message: ' + it
 * }
 * </pre>
 * If not message arrives within the given timeout, the onTimeout() lifecycle handler is invoked, if exists,
 * and the actor terminates.
 * Each PooledActor has at any point in time at most one active instance of ActorAction associated, which abstracts
 * the current chunk of actor's work to perform. Once a thread is assigned to the ActorAction, it moves the actor forward
 * till loop() or react() is called. These methods schedule another ActorAction for processing and throw dedicated exception
 * to terminate the current ActorAction. 
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
    private final AtomicBoolean stopFlag = new AtomicBoolean(true)

    /**
     * Code for the loop, if any
     */
    private final AtomicReference<Closure> loopCode = new AtomicReference<Closure>(null)

    /**
     * The current active action (continuation) associated with the actor. An action must not use Actor's state
     * after it schedules a new action, only throw CONTINUE.
     */
    final AtomicReference<ActorAction> currentAction = new AtomicReference<ActorAction>(null)

    /**
     * The current timeout task, which will send a TIMEOUT message to the actor if not cancelled by someone
     * calling the send() method within the timeout specified for the currently blocked react() method.
     */
    private AtomicReference<TimerTask> timerTask = new AtomicReference<TimerTask>(null)

    /**
     * Internal lock to synchronize access of external threads calling send() or stop() with the current active actor action
     */
    private final Object lock = new Object()

    /**
     * Timer holding timeouts for react methods
     */
    private static final Timer timer = new Timer(true)

    /**
     * Starts the Actor. No messages can be send or received before an Actor is started.
     */
    public final AbstractPooledActor start() {
        if (!stopFlag.getAndSet(false)) throw new IllegalStateException("Actor has alredy been started.")
        actorAction(this) {
            if (delegate.respondsTo('afterStart')) delegate.afterStart()
            act()
        }
        return this
    }

    /**
     * Sets the stopFlag
     * @return The previous value of the stopFlag
     */
    final boolean indicateStop() {
        cancelCurrentTimeoutTimer('')
        return stopFlag.getAndSet(true)
    }

    /**
     * Stops the Actor. The background thread will be interrupted, unprocessed messages will be passed to the afterStop
     * method, if exists.
     * Has no effect if the Actor is not started.
     * @return The actor
     */
    public final Actor stop() {
        synchronized (lock) {
            if (!indicateStop()) {
                (codeReference.getAndSet(null)) ? actorAction(this) { throw TERMINATE } : currentAction.get()?.cancel()
            }
        }
        return this
    }

    /**
     * Checks the current status of the Actor.
     */
    public final boolean isActive() {
        return !stopFlag.get()
    }

    /**
     * This method represents the body of the actor. It is called upon actor's start and can exit either normally
     * by return or due to actor being stopped through the stop() method, which cancels the current actor action.
     * Provides an extension point for subclasses to provide their custom Actor's message handling code.
     * The default implementation throws UnsupportedOperationException.
     */
    protected void act() {
        throw new UnsupportedOperationException("The act() method must be overriden")
    }

    /**
     * Schedules an ActorAction to take the next message off the message queue and to pass it on to the supplied closure.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     * @param duration Time to wait at most for a message to arrive. The actor terminates if a message doesn't arrive within the given timeout.
     * The TimeCategory DSL to specify timeouts is available inside the Actor's act() method.
     * @param code The code to handle the next message. The reply() and replyIfExists() methods are available inside
     * the closure to send a reply back to the actor, which sent the original message.
     */
    protected final void react(final Duration duration, final Closure code) {
        react(duration.toMilliseconds(), code)
    }

    /**
     * Schedules an ActorAction to take the next message off the message queue and to pass it on to the supplied closure.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     * @param code The code to handle the next message. The reply() and replyIfExists() methods are available inside
     * the closure to send a reply back to the actor, which sent the original message.
     */
    protected final void react(final Closure code) {
        react(0, code)
    }

    /**
     * Schedules an ActorAction to take the next message off the message queue and to pass it on to the supplied closure.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     * @param timeout Time in miliseconds to wait at most for a message to arrive. The actor terminates if a message doesn't arrive within the given timeout.
     * @param code The code to handle the next message. The reply() and replyIfExists() methods are available inside
     * the closure to send a reply back to the actor, which sent the original message.
     */
    protected final void react(final long timeout, final Closure code) {

        Closure reactCode = {ActorMessage message ->
            if (message.payLoad == TIMEOUT) throw TIMEOUT
            AbstractPooledActor.enhanceWithReplyMethods(this, message.sender)
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
                    timerTask.set([run: { this.send(TIMEOUT) }] as TimerTask)
                    timer.schedule(timerTask.get(), timeout)
                }
            }
        }
        throw CONTINUE
    }

    /**
     * Adds a message to the Actor's queue. Can only be called on a started Actor.
     * If there's no ActorAction scheduled for the actor a new one is created and scheduled on the thread pool.
     */
    public final Actor send(Object message) {
        synchronized (lock) {
            checkState()
            cancelCurrentTimeoutTimer(message)

            def actorMessage = new ActorMessage(message, ActorAction.currentActorPerThread.get())

            final Closure currentReference = codeReference.getAndSet(null)
            if (currentReference) {
                actorAction(this) { currentReference.call(actorMessage) }
            } else {
                messageQueue.put(actorMessage)
            }
        }
        return this
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
     * Ensures that the suplied closure will be invoked repeatedly in a loop.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     * @param code The closure to invoke repeatedly
     */
    protected final void loop(final Closure code) {
        assert loopCode.get() == null, "The loop method must be only called once"
        loopCode.set(code)
        doLoopCall(code)
    }

    /**
     * Plans another loop iteration
     */
    protected final void repeatLoop() {
        final Closure code = loopCode.get()
        if (!code) return;
        doLoopCall(code)
    }

    private def doLoopCall(Closure code) {
        if (stopFlag.get()) throw TERMINATE
        actorAction(this, code)
        throw CONTINUE
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

    /**
     * Adds reply() and replyIfExists() methods to the currentActor.
     * These methods will call send() on the target actor (the sender of the original message)
     * @param currentActor The actor to enhance
     * @param sender The actor that we need to be able to respond to
     */
    private static void enhanceWithReplyMethods(PooledActor currentActor, Actor sender) {
        currentActor.metaClass {
            reply = {
                if (sender) {
                    sender.send it
                } else {
                    throw new IllegalArgumentException("Cannot send a message ${it} to a null recipient.")
                }
            }

            replyIfExists = {
                try {
                    sender?.send it
                } catch (IllegalStateException ignore) { }
            }
        }
    }

    //todo retry after timeout or exception
    //todo support mixins
    //todo implement reply for thread-bound actors and between the two actor categories
    //todo handle nested loops
}