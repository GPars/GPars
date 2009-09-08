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

package org.gparallelizer.actors

import groovy.time.Duration
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.codehaus.groovy.runtime.TimeCategory
import org.gparallelizer.actors.*

/**
 * AbstractThreadActor provides the default thread-bound actor implementation. It represents a standalone active object (actor),
 * which reacts asynchronously to messages sent to it from outside through the send() method.
 * Each Actor has its own message queue and a thread that runs actor's body. The thread is taken from a pool at actor's
 * start time and returned back to the pool after the actor termminates. The pool is shared with other AbstractThreadActors
 * by means of an instance of the ActorGroup, which they have in common.
 * The ActorGroup instance is responsible for the pool creation, management and shutdown.
 * Whenever a ThreadActor looks for a new message through the receive() method, the actor's thread gets blocked
 * until a message arrives into the actors message queue. Since the thread is physically blocked in the receive() method,
 * it cannot be reused for other actors and so the thread pool for thread-bound actors automatically resizes
 * to the number of active actore it the group.
 * Use pooled actors instead of thread-bound actors if the number of cuncurrently run thread is of a concern.
 * On the other hand, thread-bound actors are generaly faster than pooled actors.
 * The AbstractThreadActor class is the default Actor implementation.
 * It is designed with extensibility in mind and so different message queues as well as the act method body can be
 * specified by subclasses. The actor must be started after construction by calling the start() method. This will
 * associate a thread with the actor.
 * The afterStart() method of the Actor, if the method exists, is called and then the thread keeps
 * calling the act() method, until the stop() method is called or the actor thread is interrupted directly.
 * After it stops the afterStop(List unprocessedMessages) is called, if exists,
 * with all the unprocessed messages from the queue as a parameter.
 * The Actor cannot be restarted after stopped.
 *
 * The receive method can acept multiple messages in the passed-in closure
 * <pre>
 * receive {Integer a, String b ->
 *     ...
 * }
 * </pre>
 * The closures passed to the receive() method can call reply() or replyIfExists(), which will send a message back to
 * the originator of the currently processed message. The replyIfExists() method unlike the reply() method will not fail
 * if the original message wasn't sent by an actor nor if the original sender actor is no longer running.
 * The reply() and replyIfExists() methods are also dynamically added to the processed messages.
 * <pre>
 * receive {a, b ->
 *     reply 'message'  //sent to senders of a as well as b
 *     a.reply 'private message'  //sent to the sender of a only
 * }
 *
 * def c = receive()
 * reply 'message'  //sent to the sender of c
 * c.reply 'message'  //sent to the sender of c
 * </pre>
 * To speed up actor message processing enhancing messges and actors with reply methods can be disabled by calling
 * the disableSendingReplies() method. Calling enableSendingReplies() will initiate enhancements for reply again.
 *
 * The receive() method accepts timout specified using the TimeCategory DSL.
 * <pre>
 * receive(10.MINUTES) {
 *     println 'Received message: ' + it
 * }
 * </pre>
 * If no message arrives within the given timeout, the onTimeout() lifecycle handler is invoked, if exists,
 * and the actor terminates.
 * Each Actor can define lifecycle observing methods, which will be called by the Actor's background thread whenever a certain lifecycle event occurs.
 * <ul>
 * <li>afterStart() - called immediatelly after the Actor's background thread has been started, before the act() method is called the first time.</li>
 * <li>afterStop(List undeliveredMessages) - called right after the actor is stopped, passing in all the messages from the queue.</li>
 * <li>onInterrupt(InterruptedException e) - called when the actor's thread gets interrupted. Thread interruption will result in the stopping the actor in any case.</li>
 * <li>onException(Throwable e) - called when an exception occurs in the actor's thread. Throwing an exception from this method will stop the actor.</li>
 * </ul>
 *
 * @author Vaclav Pech
 * Date: Jan 7, 2009
 */
abstract public class AbstractThreadActor extends CommonActorImpl implements ThreadedActor {

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
     * Sets the actorThread to the current thread.
     */
    protected final void adjustActorThreadAfterStart() {
        this.actorThread = Thread.currentThread()
    }

    /**
     * Sets the actorThread to null and clears the interrupted flag on the current flag.
     */
    protected final void clearActorThread() {
        this.actorThread = null
        final Thread thread = Thread.currentThread()
        thread.interrupted()
    }

    /**
     * Flag indicating Actor's liveness status.
     */
    private final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * Flag indicating that an actor has been stopped.
     */
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    /**
     * Creates a new Actor using the passed-in queue to store incoming messages.
     */
    public AbstractThreadActor(final BlockingQueue<ActorMessage> messageQueue) {
        if (messageQueue == null) throw new IllegalArgumentException("Actor message queue must not be null.")
        this.messageQueue = messageQueue;
        actorGroup = Actors.defaultActorGroup
    }

    /**
     * Starts the Actor. No messages can be send or received before an Actor is started.
     */
    public final Actor start() {

        //todo should be inlined but currently it wouldn't be visible inside the closure if mixin is used
        def localStarted = started
        def localStopped = stopped

        if (localStarted.getAndSet(true)) throw new IllegalStateException("Actor already started")
        if (stopped.get()) throw new IllegalStateException("Actor cannot be restarted")

        disableGroupMembershipChange()

        final CountDownLatch actorBarrier = new CountDownLatch(1)

        actorGroup.execute {
            adjustActorThreadAfterStart()
            actorBarrier.countDown()
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
                senders.clear()
                localStopped.set(true)
                localStarted.set(false)

                try {
                    final List queuedMessages = sweepQueue()
                    if (this.respondsTo('afterStop')) this.afterStop(queuedMessages)
                } catch (Throwable e) {
                    try {
                        reportError(delegate, e)
                    } catch (Throwable ex) {ex.printStackTrace(System.err)} //invoked when the onException handler threw an exception
                } finally {
                    releaseJoinedThreads()
                    clearActorThread()
                }
                ReplyRegistry.deregisterCurrentActorWithThread()
            }
        } as Runnable
        actorBarrier.await()
        return this
    }

    /**
     * Releases the latch with all threads that have called join on the actor
     */
    final void releaseJoinedThreads() {
        joinLatch.countDown()
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

    /**
     * Checks whether the current thread is the actor's current thread.
     */
    public final boolean isActorThread() {
        return Thread.currentThread() == actorThread
    }

    //todo should be private but mixins woudn't work
    /**
     * Does the actual message receive using the supplied closure and wraps it with all necessary ceremony
     */
    final ActorMessage doReceive(Closure code) throws InterruptedException {
        checkState();
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
    protected final Object receive() throws InterruptedException {
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
    protected final Object receive(long timeout, TimeUnit timeUnit) throws InterruptedException {
        Object message = doReceive {messageQueue.poll(timeout, timeUnit)}
        enhanceReplies([message])
        return message?.payLoad
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
     * Adds the message to the Actor's message queue.
     * The method will wait for space to become available in the queue, if it is full.
     * It can only be called on a started Actor.
     * @return The same Actor instance
     * @throws InterruptedException If the thread is interrupted during the wait.
     */
    public final Actor send(Object message) {
        checkState()
        messageQueue.put(ActorMessage.build(message))
        return this
    }

    /**
     * Sends a message and waits for a reply.
     * Returns the reply or throws an IllegalStateException, if the target actor cannot reply.
     * @return The message that came in reply to the original send.
     */
    public final sendAndWait(Object message) {
        new SendAndWaitThreadedActor(this, message).start().result
    }

    /**
     * Sends a message and waits for a reply. Timeouts after the specified timeout. In case of timeout returns null.
     * Returns the reply or throws an IllegalStateException, if the target actor cannot reply.
     * @return The message that came in reply to the original send.
     */
    public final sendAndWait(long timeout, TimeUnit timeUnit, Object message) {
        new SendAndWaitThreadedActor(this, message, timeUnit.toMillis(timeout)).start().result
    }

    /**
     * Sends a message and waits for a reply. Timeouts after the specified timeout. In case of timeout returns null.
     * Returns the reply or throws an IllegalStateException, if the target actor cannot reply.
     * @return The message that came in reply to the original send.
     */
    public final sendAndWait(Duration duration, Object message) {
        return sendAndWait(duration.toMilliseconds(), TimeUnit.MILLISECONDS, message)
    }

    /**
     * Adds the message to the Actor's message queue.
     * The method will wait for space to become available in the queue, if it is full.
     * It can only be called on a started Actor.
     * @return The same Actor instance
     * @throws InterruptedException If the thread is interrupted during the wait.
     */
    public final Actor leftShift(Object message) throws InterruptedException { send message }

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
        ActorMessage message = messageQueue.poll()
        while (message != null) {
            if (message.payLoad.respondsTo('onDeliveryError')) message.payLoad.onDeliveryError()
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
}

/**
 * Sends a message to the specified actor and waits for reply.
 * The message is enhanced to send notification in case the target actor terminates without processing the message.
 * Exceptions are re-throvn from the getResult() method.
 */
final class SendAndWaitThreadedActor extends DefaultThreadActor {

    final private Actor targetActor
    final private Object message
    final private CountDownLatch actorBarrier = new CountDownLatch(1)
    private Object result
    private long timeout = -1

    def SendAndWaitThreadedActor(final targetActor, final message) {
        this.targetActor = targetActor;
        this.message = message
        this.actorGroup = targetActor.actorGroup
    }

    def SendAndWaitThreadedActor(final targetActor, final message, final long timeout) {
        this(targetActor, message)
        this.timeout = timeout
    }

  void act() {
        message.getMetaClass().onDeliveryError = {->
            this << new IllegalStateException('Cannot deliver the message. The target actor may not be active.')
        }

        try {
            targetActor << message
            result = (timeout < 0) ? receive() : receive(timeout, TimeUnit.MILLISECONDS)
        } catch (Exception e) {
            result = e
        } finally {
            actorBarrier.countDown()
            stop()
        }
    }

    /**
     * Retrieves the result, waiting for it, if needed.
     * Non-blocking under Fork/oin pool.
     */
    Object getResult() {
        actorBarrier.await()
        if (result instanceof Exception) throw result else return result
    }
}
