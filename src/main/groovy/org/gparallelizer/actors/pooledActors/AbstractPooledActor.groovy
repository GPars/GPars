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

package org.gparallelizer.actors.pooledActors

import groovy.time.Duration
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.gparallelizer.actors.Actor
import org.gparallelizer.actors.ActorMessage
import org.gparallelizer.actors.CommonActorImpl
import static org.gparallelizer.actors.pooledActors.ActorAction.actorAction
import static org.gparallelizer.actors.pooledActors.ActorException.*
import org.gparallelizer.MessageStream

/**
 * AbstractPooledActor provides the default PooledActor implementation. It represents a standalone active object (actor),
 * which reacts asynchronously to messages sent to it from outside through the send() method.
 * Each PooledActor has its own message queue and a thread pool shared with other PooledActors by means of an instance
 * of the PooledActorGroup, which they have in common.
 * The PooledActorGroup instance is responsible for the pool creation, management and shutdown.
 * All work performed by a PooledActor is divided into chunks, which are sequentially submitted as independent tasks
 * to the thread pool for processing.
 * Whenever a PooledActor looks for a new message through the react() method, the actor gets detached
 * from the thread, making the thread available for other actors. Thanks to the ability to dynamically attach and detach
 * threads to actors, PooledActors can scale far beyond the limits of the underlying platform on number of concurrently
 * available threads.
 * The loop() method allows to repeatedly invoke a closure and yet perform each of the iterations sequentially
 * in different thread from the thread pool.
 * To support continuations correctly the react() and loop() methods never return.
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
 *}.start()
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
 *}.start()
 * </pre>
 * The react method can accept multiple messages in the passed-in closure
 * <pre>
 * react {Integer a, String b ->
 *     ...
 * }
 * </pre>
 * The closures passed to the react() method can call reply() or replyIfExists(), which will send a message back to
 * the originator of the currently processed message. The replyIfExists() method unlike the reply() method will not fail
 * if the original message wasn't sent by an actor nor if the original sender actor is no longer running.
 * The reply() and replyIfExists() methods are also dynamically added to the processed messages.
 * <pre>
 * react {a, b ->
 *     reply 'message'  //sent to senders of a as well as b
 *     a.reply 'private message'  //sent to the sender of a only
 * }
 * </pre>
 * To speed up actor message processing enhancing messages and actors with reply methods can be disabled by calling
 * the disableSendingReplies() method. Calling enableSendingReplies() will initiate enhancements for reply again.
 *
 * The react() method accepts timeout specified using the TimeCategory DSL.
 * <pre>
 * react(10.MINUTES) {
 *     println 'Received message: ' + it
 * }
 * </pre>
 * If no message arrives within the given timeout, the onTimeout() lifecycle handler is invoked, if exists,
 * and the actor terminates.
 * Each PooledActor has at any point in time at most one active instance of ActorAction associated, which abstracts
 * the current chunk of actor's work to perform. Once a thread is assigned to the ActorAction, it moves the actor forward
 * till loop() or react() is called. These methods schedule another ActorAction for processing and throw dedicated exception
 * to terminate the current ActorAction. 
 *
 * Each Actor can define lifecycle observing methods, which will be called by the Actor's background thread whenever a certain lifecycle event occurs.
 * <ul>
 * <li>afterStart() - called immediately after the Actor's background thread has been started, before the act() method is called the first time.</li>
 * <li>afterStop(List undeliveredMessages) - called right after the actor is stopped, passing in all the messages from the queue.</li>
 * <li>onInterrupt(InterruptedException e) - called when a react() method timeouts. The actor will be terminated.
 * <li>onTimeout() - called when the actor's thread gets interrupted. Thread interruption will result in the stopping the actor in any case.</li>
 * <li>onException(Throwable e) - called when an exception occurs in the actor's thread. Throwing an exception from this method will stop the actor.</li>
 * </ul>
 * @author Vaclav Pech
 * Date: Feb 7, 2009
 */
abstract public class AbstractPooledActor extends PooledActor {

    /**
     * Queue for the messages
     */
    final BlockingQueue messageQueue = new LinkedBlockingQueue();

    /**
     * Code for the next action
     */
    private final AtomicReference<Closure> codeReference = new AtomicReference<Closure>(null)

    /**
     * Buffer to hold messages required by the scheduled next continuation. Null when no continuation scheduled.
     */
    private MessageHolder bufferedMessages = null

    //todo should be private, but wouldn't work
    /**
     * A copy of buffer in case of timeout.
     */
    List savedBufferedMessages = null

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

    def AbstractPooledActor() {
        actorGroup = PooledActors.defaultPooledActorGroup
    }

    /**
     * Starts the Actor. No messages can be send or received before an Actor is started.
     */
    public final AbstractPooledActor start() {
        disableGroupMembershipChange()
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
    @Override
    public final boolean isActive() {
        return !stopFlag.get()
    }

    /**
     * Checks whether the current thread is the actor's current thread.
     */
    public final boolean isActorThread() {
        return Thread.currentThread() == currentAction.get()?.actionThread
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
        react(-1, code)
    }

    /**
     * Schedules an ActorAction to take the next message off the message queue and to pass it on to the supplied closure.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     * @param timeout Time in milliseconds to wait at most for a message to arrive. The actor terminates if a message doesn't arrive within the given timeout.
     * @param timeUnit a TimeUnit determining how to interpret the timeout parameter
     * @param code The code to handle the next message. The reply() and replyIfExists() methods are available inside
     * the closure to send a reply back to the actor, which sent the original message.
     */
    protected final void react(final long timeout, TimeUnit timeUnit, final Closure code) {
        react(timeUnit.toMillis(timeout), code)
    }

    /**
     * Schedules an ActorAction to take the next message off the message queue and to pass it on to the supplied closure.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     * Also adds reply() and replyIfExists() methods to the currentActor and the message.
     * These methods will call send() on the target actor (the sender of the original message).
     * The reply()/replyIfExists() methods invoked on the actor will be sent to all currently processed messages,
     * reply()/replyIfExists() invoked on a message will send a reply to the sender of that particular message only.
     * @param timeout Time in milliseconds to wait at most for a message to arrive. The actor terminates if a message doesn't arrive within the given timeout.
     * @param code The code to handle the next message. The reply() and replyIfExists() methods are available inside
     * the closure to send a reply back to the actor, which sent the original message.
     */
    protected final void react(final long timeout, final Closure code) {

        senders.clear()
        final int maxNumberOfParameters = code.maximumNumberOfParameters

        Closure reactCode = {List<ActorMessage> messages ->

            if (messages.any {ActorMessage actorMessage -> (TIMEOUT == actorMessage.payLoad)}) {
                savedBufferedMessages = messages.findAll {it != null && TIMEOUT != it.payLoad}*.payLoad
                throw TIMEOUT
            }

            if (sendRepliesFlag) {
                for (message in messages) {
                    senders << message?.sender
                }
                enhanceWithReplyMethodsToMessages(messages)
            }
            maxNumberOfParameters > 0 ? code.call(* (messages*.payLoad)) : code.call()
            this.repeatLoop()
        }
        code.resolveStrategy=Closure.DELEGATE_FIRST
        code.delegate = this

        synchronized (lock) {
            if (stopFlag.get()) throw TERMINATE
            assert (codeReference.get() == null), "Cannot have more react called at the same time."

            bufferedMessages = new MessageHolder(maxNumberOfParameters)

            ActorMessage currentMessage
            while ((!bufferedMessages.ready) && (currentMessage = (ActorMessage) messageQueue.poll())) {
                bufferedMessages.addMessage(currentMessage)
            }
            if (bufferedMessages.ready) {
                final List<ActorMessage> messages = bufferedMessages.messages
                actorAction(this) { reactCode.call(messages) }
                bufferedMessages = null
            } else {
                codeReference.set(reactCode)
                if (timeout >= 0) {
                    timerTask.set([run: { this.send(TIMEOUT) }] as TimerTask)
                    timer.schedule(timerTask.get(), timeout)
                }
            }
        }
        throw CONTINUE
    }

    //todo should be private but mixins woudn't work
    /**
     * Does the actual message receive using the supplied closure and wraps it with all necessary ceremony
     */
    final ActorMessage doReceive(Closure code) throws InterruptedException {
        if (stopFlag.get()) throw new IllegalStateException("The actor hasn't been started.");
        return code()
    }

    //todo should be private, but woudn't be work
    /**
     * Adds reply() and replyIfExists() methods to the currentActor and the message.
     * These methods will call send() on the target actor (the sender of the original message).
     * The reply()/replyIfExists() methods invoked on the actor will be sent to all currently processed messages,
     * reply()/replyIfExists() invoked on a message will send a reply to the sender of that particular message only.
     * @param message The instance of ActorMessage wrapping the sender actor, who we need to be able to respond to,
     * plus the original message
     */
    final void enhanceReplies(List<ActorMessage> messages) {
        senders.clear()
        if (sendRepliesFlag) {
            for (message in messages) {
                senders << message?.sender
            }
            enhanceWithReplyMethodsToMessages(messages)
        }
    }

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     * @return The message retrieved from the queue.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    protected final Object receiveImpl() throws InterruptedException {
        Object message = doReceive {messageQueue.take()}
        enhanceReplies([message])
        return message?.payLoad
    }

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     * @param timeout how long to wait before giving up, in units of unit
     * @param timeUnit a TimeUnit determining how to interpret the timeout parameter
     * @return The message retrieved from the queue, or null, if the timeout expires.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    protected final Object receiveImpl(long timeout, TimeUnit timeUnit) throws InterruptedException {
        Object message = doReceive {messageQueue.poll(timeout, timeUnit)}
        enhanceReplies([message])
        return message?.payLoad
    }

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     * The message retrieved from the queue is passed into the handler as the only parameter.
     * @param handler A closure accepting the retrieved message as a parameter, which will be invoked after a message is received.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    protected final void receive(Closure handler) throws InterruptedException {
        handler.resolveStrategy=Closure.DELEGATE_FIRST
        handler.delegate = this
        int maxNumberOfParameters = handler.maximumNumberOfParameters
        if (maxNumberOfParameters == 0) {
            ActorMessage message = doReceive {messageQueue.take()}
            try {
                enhanceReplies([message])
                handler.call()
            } finally {
                senders.clear()
            }

        } else {
            final List<ActorMessage> messages = []
            for (i in 1..maxNumberOfParameters) { messages << doReceive {messageQueue.take()} }
            try {
                enhanceReplies(messages)
                handler.call(* messages*.payLoad)
            } finally {
                senders.clear()
            }
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
        handler.resolveStrategy=Closure.DELEGATE_FIRST
        handler.delegate = this
        int maxNumberOfParameters = handler.maximumNumberOfParameters
        if (maxNumberOfParameters == 0) {
            ActorMessage message = doReceive {messageQueue.poll(timeout, timeUnit)}
            try {
                enhanceReplies([message])
                handler.call()
            } finally {
                senders.clear()
            }

        } else {
            long stopTime = timeUnit.toMillis(timeout) + System.currentTimeMillis()
            boolean nullAppeared = false  //Ignore further potential messages once a null is retrieved (due to a timeout)

            final List<ActorMessage> messages = []
            for (i in 1..maxNumberOfParameters) {
                if (nullAppeared) messages << null
                else {
                    ActorMessage message = doReceive {
                        messageQueue.poll(Math.max(stopTime - System.currentTimeMillis(), 0), TimeUnit.MILLISECONDS)
                    }
                    nullAppeared = (message == null)
                    messages << message
                }
            }
            try {
                enhanceReplies(messages)
                handler.call(* messages*.payLoad)
            } finally {
                senders.clear()
            }
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
        receive(duration.toMilliseconds(), TimeUnit.MILLISECONDS, handler)
    }

    /**
     * Releases the latch with all threads that have called join on the actor
     */
    final void releaseJoinedThreads() { joinLatch.countDown() }

    /**
     * Adds a message to the Actor's queue. Can only be called on a started Actor.
     * If there's no ActorAction scheduled for the actor a new one is created and scheduled on the thread pool.
     */
    public final Actor send(Object message) {
        synchronized (lock) {
            if (stopFlag.get()) throw new IllegalStateException("The actor hasn't been started.");
            cancelCurrentTimeoutTimer(message)

            def actorMessage
            if(message instanceof ActorMessage)
              actorMessage = message;
            else
              actorMessage = ActorMessage.build(message);

            final Closure currentReference = codeReference.get()
            if (currentReference) {
                assert bufferedMessages && !bufferedMessages.isReady()
                bufferedMessages.addMessage actorMessage
                if (bufferedMessages.ready) {
                    final List<ActorMessage> messages = bufferedMessages.messages
                    codeReference.set(null)
                    bufferedMessages = null
                    actorAction(this) { currentReference.call(messages) }
                }
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
        if (savedBufferedMessages) messages.addAll savedBufferedMessages
        ActorMessage message = messageQueue.poll()
        while (message != null) {
          if (message.payLoad.respondsTo('onDeliveryError'))
            message.payLoad.onDeliveryError()
          else
            if (message.sender.respondsTo('onDeliveryError'))
              message.sender.onDeliveryError ()
            messages << message
            message = messageQueue.poll()
        }
        return messages
    }

    /**
     * Ensures that the supplied closure will be invoked repeatedly in a loop.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     * @param code The closure to invoke repeatedly
     */
    protected final void loop(final Closure code) {
        assert loopCode.get() == null, "The loop method must be only called once"
        final Closure enhancedCode = {code(); repeatLoop()}
        enhancedCode.resolveStrategy=Closure.DELEGATE_FIRST
        enhancedCode.delegate = this
        loopCode.set(enhancedCode)
        doLoopCall(enhancedCode)
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
        if (TIMEOUT != message) timerTask.get()?.cancel()
    }
}
