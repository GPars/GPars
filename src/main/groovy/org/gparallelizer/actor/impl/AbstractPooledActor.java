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

package org.gparallelizer.actor.impl;

import groovy.lang.Closure;
import groovy.lang.GroovyRuntimeException;
import groovy.time.Duration;
import org.codehaus.groovy.runtime.*;
import org.gparallelizer.MessageStream;
import org.gparallelizer.actor.Actor;
import org.gparallelizer.actor.ActorGroup;
import org.gparallelizer.actor.ActorMessage;
import org.gparallelizer.actor.Actors;
import static org.gparallelizer.actor.impl.ActorException.*;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

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
 * <p/>
 * def actor = actor {
 *     loop {
 *         react {message ->
 *             println message
 *         }
 *         //this line will never be reached
 *     }
 *     //this line will never be reached
 * }.start()
 * <p/>
 * actor.send 'Hi!'
 * </pre>
 * This requires the code to be structured accordingly.
 * <p/>
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
 * <p/>
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
 * <p/>
 * Each Actor can define lifecycle observing methods, which will be called by the Actor's background thread whenever a certain lifecycle event occurs.
 * <ul>
 * <li>afterStart() - called immediately after the Actor's background thread has been started, before the act() method is called the first time.</li>
 * <li>afterStop(List undeliveredMessages) - called right after the actor is stopped, passing in all the messages from the queue.</li>
 * <li>onInterrupt(InterruptedException e) - called when a react() method timeouts. The actor will be terminated.
 * <li>onTimeout() - called when the actor's thread gets interrupted. Thread interruption will result in the stopping the actor in any case.</li>
 * <li>onException(Throwable e) - called when an exception occurs in the actor's thread. Throwing an exception from this method will stop the actor.</li>
 * </ul>
 *
 * @author Vaclav Pech, Alex Tkachman, Dierk Koenig
 *         Date: Feb 7, 2009
 */
abstract public class AbstractPooledActor extends Actor {
    /**
     * The actor group to which the actor belongs
     */
    private volatile ActorGroup actorGroup;

    /**
     * Indicates whether the actor's group can be changed. It is typically not changeable after actor starts.
     */
    private volatile boolean groupMembershipChangeable = true;

    /**
     * Queue for the messages
     */
    final BlockingQueue<ActorMessage> messageQueue = new LinkedBlockingQueue<ActorMessage>();

    /**
     * Code for the next action
     */
    private volatile Reaction reaction;

    private static final AtomicReferenceFieldUpdater<AbstractPooledActor, Reaction> reactionUpdater = AtomicReferenceFieldUpdater.newUpdater(AbstractPooledActor.class, Reaction.class, "reaction");

    /**
     * A copy of buffer in case of timeout.
     */
    private List<ActorMessage> savedBufferedMessages;


    private static final int S_STOPPED = 0;
    private static final int S_RUNNING = 1;
    private static final int S_STOPPING = 2;

    /**
     * Indicates whether the actor should terminate
     */
    private volatile int stopFlag = S_STOPPED;

    private static final AtomicIntegerFieldUpdater<AbstractPooledActor> stopFlagUpdater = AtomicIntegerFieldUpdater.newUpdater(AbstractPooledActor.class, "stopFlag");

    /**
     * Code for the loop, if any
     */
    private volatile Runnable loopCode;

    /**
     * The current active action (continuation) associated with the actor. An action must not use Actor's state
     * after it schedules a new action, only throw CONTINUE.
     */
    private volatile ActorAction currentAction;

    /**
     * Timer holding timeouts for react methods
     */
    private static final Timer timer = new Timer(true);

    public AbstractPooledActor() {
        setActorGroup(Actors.defaultPooledActorGroup);
    }

    /**
     * Disallows any subsequent changes to the group attached to the actor.
     */
    protected final void disableGroupMembershipChange() {
        groupMembershipChangeable = false;
    }

    /**
     * Sets the actor's group.
     * It can only be invoked before the actor is started.
     *
     * @param group new group
     */
    public final void setActorGroup(final ActorGroup group) {
        if (!groupMembershipChangeable)
            throw new IllegalStateException("Cannot set actor's group on a started actor.");

        if (group == null)
            throw new IllegalArgumentException("Cannot set actor's group to null.");

        actorGroup = group;
    }

    /**
     * Retrieves the group to which the actor belongs
     *
     * @return The actor's group
     */
    public ActorGroup getActorGroup() {
        return actorGroup;
    }

    private void schedule(ActorAction action) {
        final ActorAction ca = currentAction;
        if (ca != null)
            ca.schedule(action);
        else
            getActorGroup().getThreadPool().execute(action);
    }

    /**
     * Starts the Actor. No messages can be send or received before an Actor is started.
     *
     * @return this (the actor itself) to allow method chaining
     */
    public final AbstractPooledActor start() {
        disableGroupMembershipChange();
        if (!stopFlagUpdater.compareAndSet(this, S_STOPPED, S_RUNNING))
            throw new IllegalStateException("Actor has already been started.");

        currentAction = null;
        reaction = null;
        loopCode = null;
        getJoinLatch().rebind();

        schedule(new ActorAction(this, new Runnable() {
            public void run() {
                onStart();
                act();
            }
        }));
        return this;
    }

    protected void onStart() {
        final Object list = InvokerHelper.invokeMethod(this, "respondsTo", new Object[]{"afterStart"});
        if (list != null && !((List) list).isEmpty())
            InvokerHelper.invokeMethod(this, "afterStart", new Object[]{});
    }

    /**
     * Stops the Actor. The background thread will be interrupted, unprocessed messages will be passed to the afterStop
     * method, if exists.
     * Has no effect if the Actor is not started.
     *
     * @return this (the actor itself) to allow method chaining
     */
    public final Actor stop() {
        if (stopFlagUpdater.compareAndSet(this, S_RUNNING, S_STOPPING)) {
            final ActorAction action = currentAction;
            if (action != null)
                action.cancel();
            else {
                getActorGroup().getThreadPool().execute(new ActorAction(this, new Runnable() {
                    public void run() {
                        throw TERMINATE;
                    }
                }));
            }
        }
        return this;
    }

    /**
     * Checks the current status of the Actor.
     */
    @Override
    public final boolean isActive() {
        return stopFlag != S_STOPPED;
    }

    /**
     * Checks whether the current thread is the actor's current thread.
     */
    public final boolean isActorThread() {
        final ActorAction action = currentAction;
        return action != null && Thread.currentThread() == action.actionThread;
    }

    /**
     * This method represents the body of the actor. It is called upon actor's start and can exit either normally
     * by return or due to actor being stopped through the stop() method, which cancels the current actor action.
     * Provides an extension point for subclasses to provide their custom Actor's message handling code.
     * The default implementation throws UnsupportedOperationException.
     */
    protected void act() {
        throw new UnsupportedOperationException("The act() method must be overriden");
    }

    /**
     * Schedules an ActorAction to take the next message off the message queue and to pass it on to the supplied closure.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     *
     * @param duration Time to wait at most for a message to arrive. The actor terminates if a message doesn't arrive within the given timeout.
     *                 The TimeCategory DSL to specify timeouts is available inside the Actor's act() method.
     * @param code     The code to handle the next message. The reply() and replyIfExists() methods are available inside
     *                 the closure to send a reply back to the actor, which sent the original message.
     */
    protected final void react(final Duration duration, final Closure code) {
        react(duration.toMilliseconds(), code);
    }

    /**
     * Schedules an ActorAction to take the next message off the message queue and to pass it on to the supplied closure.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     *
     * @param code The code to handle the next message. The reply() and replyIfExists() methods are available inside
     *             the closure to send a reply back to the actor, which sent the original message.
     */
    protected final void react(final Closure code) {
        react(-1, code);
    }

    /**
     * Schedules an ActorAction to take the next message off the message queue and to pass it on to the supplied closure.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     *
     * @param timeout  Time in milliseconds to wait at most for a message to arrive. The actor terminates if a message doesn't arrive within the given timeout.
     * @param timeUnit a TimeUnit determining how to interpret the timeout parameter
     * @param code     The code to handle the next message. The reply() and replyIfExists() methods are available inside
     *                 the closure to send a reply back to the actor, which sent the original message.
     */
    protected final void react(final long timeout, TimeUnit timeUnit, final Closure code) {
        react(timeUnit.toMillis(timeout), code);
    }

    /**
     * Schedules an ActorAction to take the next message off the message queue and to pass it on to the supplied closure.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     * Also adds reply() and replyIfExists() methods to the currentActor and the message.
     * These methods will call send() on the target actor (the sender of the original message).
     * The reply()/replyIfExists() methods invoked on the actor will be sent to all currently processed messages,
     * reply()/replyIfExists() invoked on a message will send a reply to the sender of that particular message only.
     *
     * @param timeout Time in milliseconds to wait at most for a message to arrive. The actor terminates if a message doesn't arrive within the given timeout.
     * @param code    The code to handle the next message. The reply() and replyIfExists() methods are available inside
     *                the closure to send a reply back to the actor, which sent the original message.
     */
    protected final void react(final long timeout, final Closure code) {

        if (!isActorThread())
            throw new IllegalStateException("Cannot call react from thread which is not owned by the actor");

        if (stopFlag != S_RUNNING)
            throw TERMINATE;

        getSenders().clear();
        final int maxNumberOfParameters = code.getMaximumNumberOfParameters();

        code.setResolveStrategy(Closure.DELEGATE_FIRST);
        code.setDelegate(this);

        assert reaction == null;

        final Reaction reactCode = new Reaction(this, maxNumberOfParameters, code);
        reactCode.checkQueue();

        assert !reactCode.isReady();

        if (timeout >= 0) {
            reactCode.setTimeout(timeout);
        }

        reaction = reactCode;
        reactCode.checkQueue(); // it could happen that some messages arrived

        throw CONTINUE;
    }

    /**
     * Adds reply() and replyIfExists() methods to the currentActor and the message.
     * These methods will call send() on the target actor (the sender of the original message).
     * The reply()/replyIfExists() methods invoked on the actor will be sent to all currently processed messages,
     * reply()/replyIfExists() invoked on a message will send a reply to the sender of that particular message only.
     *
     * @param messages List of ActorMessage wrapping the sender actor, who we need to be able to respond to,
     *                 plus the original message
     */
    private void enhanceReplies(final List<ActorMessage> messages) {
        final List<MessageStream> senders = getSenders();
        senders.clear();
        for (final ActorMessage message : messages) {
            senders.add(message == null ? null : message.getSender());
            if (message != null)
                obj2Sender.put(message.getPayLoad(), message.getSender());
        }
    }

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     *
     * @return The message retrieved from the queue.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    @Override
    protected final Object receiveImpl() throws InterruptedException {
        if (stopFlag != S_RUNNING)
            throw new IllegalStateException("The actor hasn't been started.");
        ActorMessage message = messageQueue.take();
        enhanceReplies(Arrays.asList(message));
        if (message == null)
            return null;
        return message.getPayLoad();
    }

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     *
     * @param timeout  how long to wait before giving up, in units of unit
     * @param timeUnit a TimeUnit determining how to interpret the timeout parameter
     * @return The message retrieved from the queue, or null, if the timeout expires.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    protected final Object receiveImpl(long timeout, TimeUnit timeUnit) throws InterruptedException {
        if (stopFlag != S_RUNNING)
            throw new IllegalStateException("The actor hasn't been started.");
        ActorMessage message = messageQueue.poll(timeout, timeUnit);
        enhanceReplies(Arrays.asList(message));
        if (message == null)
            return null;
        return message.getPayLoad();
    }

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     * The message retrieved from the queue is passed into the handler as the only parameter.
     *
     * @param handler A closure accepting the retrieved message as a parameter, which will be invoked after a message is received.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    protected final void receive(Closure handler) throws InterruptedException {
        handler.setResolveStrategy(Closure.DELEGATE_FIRST);
        handler.setDelegate(this);

        final List<ActorMessage> messages = new ArrayList<ActorMessage>();
        int maxNumberOfParameters = handler.getMaximumNumberOfParameters();
        int toReceive = maxNumberOfParameters == 0 ? 1 : maxNumberOfParameters;

        for (int i = 0; i != toReceive; ++i) {
            if (stopFlag != S_RUNNING)
                throw TERMINATE;

            messages.add(messageQueue.take());
        }
        enhanceReplies(messages);

        try {
            if (maxNumberOfParameters == 0)
                handler.call();
            else {
                Object args[] = new Object[messages.size()];
                for (int i = 0; i < args.length; i++) {
                    args[i] = messages.get(i).getPayLoad();
                }
                handler.call(args);
            }

        } finally {
            getSenders().clear();
        }
    }

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     * The message retrieved from the queue is passed into the handler as the only parameter.
     * A null value is passed into the handler, if the timeout expires
     *
     * @param timeout  how long to wait before giving up, in units of unit
     * @param timeUnit a TimeUnit determining how to interpret the timeout parameter
     * @param handler  A closure accepting the retrieved message as a parameter, which will be invoked after a message is received.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    protected final void receive(long timeout, TimeUnit timeUnit, Closure handler) throws InterruptedException {
        handler.setResolveStrategy(Closure.DELEGATE_FIRST);
        handler.setDelegate(this);

        int maxNumberOfParameters = handler.getMaximumNumberOfParameters();
        int toReceive = maxNumberOfParameters == 0 ? 1 : maxNumberOfParameters;

        long stopTime = timeUnit.toMillis(timeout) + System.currentTimeMillis();

        boolean nullAppeared = false;  //Ignore further potential messages once a null is retrieved (due to a timeout)
        final List<ActorMessage> messages = new ArrayList<ActorMessage>();
        for (int i = 0; i != toReceive; ++i) {
            if (nullAppeared)
                messages.add(null);
            else {
                if (stopFlag != S_RUNNING)
                    throw new IllegalStateException("The actor hasn't been started.");
                ActorMessage message =
                        messageQueue.poll(Math.max(stopTime - System.currentTimeMillis(), 0), TimeUnit.MILLISECONDS);
                nullAppeared = (message == null);
                messages.add(message);
            }
        }


        try {
            enhanceReplies(messages);

            if (maxNumberOfParameters == 0) {
                handler.call();
            } else {
                Object args[] = new Object[messages.size()];
                for (int i = 0; i < args.length; i++) {
                    final ActorMessage am = messages.get(i);
                    args[i] = am == null ? am : am.getPayLoad();
                }
                handler.call(args);
            }
        }
        finally {
            getSenders().clear();
        }
    }

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     * The message retrieved from the queue is passed into the handler as the only parameter.
     * A null value is passed into the handler, if the timeout expires
     *
     * @param duration how long to wait before giving up, in units of unit
     * @param handler  A closure accepting the retrieved message as a parameter, which will be invoked after a message is received.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    protected final void receive(Duration duration, Closure handler) throws InterruptedException {
        receive(duration.toMilliseconds(), TimeUnit.MILLISECONDS, handler);
    }

    /**
     * Adds a message to the Actor's queue. Can only be called on a started Actor.
     * If there's no ActorAction scheduled for the actor a new one is created and scheduled on the thread pool.
     *
     * @param message may be null to simply trigger the actors receive/react
     * @return this (the actor itself) to allow method chaining
     */
    public final Actor send(Object message) {
        if (stopFlag == S_STOPPED)
            throw new IllegalStateException("The actor hasn't been started.");

        ActorMessage actorMessage;
        if (message instanceof ActorMessage)
            actorMessage = (ActorMessage) message;
        else
            actorMessage = ActorMessage.build(message);

        messageQueue.offer(actorMessage);
        final Reaction reactCode = reaction;
        if (reactCode != null) {
            reactCode.checkQueue();
        }
        return this;
    }

    /**
     * Clears the message queue returning all the messages it held.
     *
     * @return The messages stored in the queue
     */
    final List sweepQueue() {
        ArrayList<ActorMessage> messages = new ArrayList<ActorMessage>();
        if (savedBufferedMessages != null && !savedBufferedMessages.isEmpty())
            messages.addAll(savedBufferedMessages);
        ActorMessage message = messageQueue.poll();
        while (message != null) {
            Object list = InvokerHelper.invokeMethod(message.getPayLoad(), "respondsTo", new Object[]{"onDeliveryError"});
            if (list != null && !((List) list).isEmpty())
                InvokerHelper.invokeMethod(message.getPayLoad(), "onDeliveryError", new Object[0]);
            else {
                list = InvokerHelper.invokeMethod(message.getSender(), "respondsTo", new Object[]{"onDeliveryError"});
                if (list != null && !((List) list).isEmpty())
                    InvokerHelper.invokeMethod(message.getSender(), "onDeliveryError", new Object[0]);
            }

            messages.add(message);
            message = messageQueue.poll();
        }
        return messages;
    }

    /**
     * Ensures that the supplied closure will be invoked repeatedly in a loop.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     *
     * @param code The closure to invoke repeatedly
     */
    protected final void loop(final Runnable code) {
        if (loopCode != null)
            throw new IllegalStateException("The loop method must be only called once");

        if (code instanceof Closure) {
            ((Closure) code).setResolveStrategy(Closure.DELEGATE_FIRST);
            ((Closure) code).setDelegate(this);
        }
        final Runnable enhancedCode = new Runnable() {
            public void run() {
                getSenders().clear();
                obj2Sender.clear();

                if (code instanceof Closure)
                    //noinspection deprecation
                    GroovyCategorySupport.use(Arrays.<Class>asList(TimeCategory.class, ReplyCategory.class), (Closure) code);
                else
                    code.run();
                doLoopCall();
            }
        };
        loopCode = enhancedCode;
        doLoopCall();
    }

    private void doLoopCall() {
        if (stopFlag != S_RUNNING)
            throw TERMINATE;
        Runnable code = loopCode;
        if (code != null) {
            schedule(new ActorAction(this, code));
            throw CONTINUE;
        }
    }

    void runReaction(List<ActorMessage> messages, int maxNumberOfParameters, Closure code) {
        for (ActorMessage message : messages) {
            if (message.getPayLoad() == TIMEOUT) {
                final ArrayList<ActorMessage> saved = new ArrayList<ActorMessage>();
                for (ActorMessage m : messages) {
                    if (m != null && m.getPayLoad() != TIMEOUT)
                        saved.add(m);
                }
                savedBufferedMessages = saved;
                throw TIMEOUT;
            }
        }

        for (ActorMessage message : messages) {
            if (message != null) {
                getSenders().add(message.getSender());
                obj2Sender.put(message.getPayLoad(), message.getSender());
            }
        }

        if (maxNumberOfParameters > 0) {
            Object args[] = new Object[messages.size()];
            for (int i = 0; i < args.length; i++) {
                final ActorMessage am = messages.get(i);
                args[i] = am == null ? null : am.getPayLoad();
            }
            code = new CurriedClosure(code, args);
        }
        //noinspection deprecation
        GroovyCategorySupport.use(Arrays.<Class>asList(TimeCategory.class, ReplyCategory.class), code);
        doLoopCall();
    }

    /**
     * ActorAction represents a chunk of work to perform on behalf of an associated PooledActor in one go.
     * A PooledActor has at most one active ActorAction associated with it at any given time, which represents the currently
     * processed chunk of actor's work.
     * ActorActions need to be scheduled for processing on a thread pool (ExecutorService), which is best achieved
     * through the actorAction() factory method. An ActorAction may create and schedule a new ActorAction to continue processing
     * another chunk of work on the actor's behalf (hence the term "continuations").
     * After a new ActorAction has been scheduled, the original ActorAction must avoid touching the actor's state
     * to avoid race conditions with the new ActorAction and should terminate quickly by throwing a dedicated lifecycle exception..
     *
     * @author Vaclav Pech, Alex Tkachman
     *         Date: Feb 7, 2009
     */
    @SuppressWarnings({"AssignmentToNull"})
    private static class ActorAction implements Runnable {

        /**
         * The code to invoke as part of this ActorAction
         */
        private final Runnable code;

        /**
         * The thread from the pool assigned to process the current ActorAction
         */
        private volatile Thread actionThread;

        /**
         * Indicates whether the cancel() method has been called
         */
        private volatile boolean cancelled;
        private final AbstractPooledActor actor;
        private ActorAction nextAction;

        /**
         * Creates a new ActorAction asociated with a PooledActor, which will eventually perform the specified code.
         *
         * @param actor
         * @param code  The code to perform on behalf of the actor
         */
        ActorAction(AbstractPooledActor actor, final Runnable code) {
            super();
            this.actor = actor;
            if (code instanceof Closure)
                ((Closure) code).setDelegate(actor);
            this.code = code;
        }

        /**
         * Performs the next chunk of work for the associated PooledActor.
         * The actual processing is wrapped with setting and unsetting all the required dependencies between the ActorAction,
         * the PooledActor and the current thread.
         * Exception thrown from the performed code may indicate desired ways to move forward, like to continue processing
         * the next work chunk, terminate the actor, handle timeout in react(),
         * thread interruption or an exception thrown from the code.
         */
        public void run() {
            try {
                if (cancelled || stopFlagUpdater.get(actor) != S_RUNNING)
                    throw TERMINATE;

                assert actor.currentAction == null;
                actor.currentAction = this;

                registerCurrentActorWithThread(actor);
                try {
                    actionThread = Thread.currentThread();

                    try {
                        code.run();
                    } catch (GroovyRuntimeException gre) {
                        throw ScriptBytecodeAdapter.unwrap(gre);
                    }
                } finally {
                    actionThread = null;
                }
                throw TERMINATE;

            } catch (ActorContinuationException continuation) {//
            } catch (ActorTerminationException termination) {
                handleTermination();
            } catch (ActorTimeoutException timeout) {
                handleTimeout();
            } catch (InterruptedException e) {
                handleInterrupt(e);
            } catch (Throwable e) {
                handleException(e);
            } finally {
                Thread.interrupted();
                deregisterCurrentActorWithThread();

                actor.currentAction = null;
                if (nextAction != null && !cancelled && stopFlagUpdater.get(actor) == S_RUNNING)
                    actor.getActorGroup().getThreadPool().execute(nextAction);
            }
        }

        /**
         * Attempts to cancel the action and interrupt the thread processing it.
         */
        void cancel() {
            cancelled = true;
            if (actionThread != null && actionThread != Thread.currentThread())
                actionThread.interrupt();
        }

        private void handleTimeout() {
            try {
                callDynamic("onTimeout", new Object[0]);
            }
            finally {
                handleTermination();
            }
        }

        @SuppressWarnings({"FeatureEnvy"})
        private void handleTermination() {
            Thread.interrupted();
            if (stopFlagUpdater.compareAndSet(actor, S_STOPPING, S_STOPPED) || stopFlagUpdater.compareAndSet(actor, S_RUNNING, S_STOPPED)) {
                try {
                    callDynamic("afterStop", new Object[]{actor.sweepQueue()});
                } finally {
                    //noinspection unchecked
                    actor.getJoinLatch().bind(null);
                }
            }
        }

        private void handleException(final Throwable exception) {
            try {
                if (!callDynamic("onException", new Object[]{exception})) {
                    System.err.println("An exception occurred in the Actor thread " + Thread.currentThread().getName());
                    exception.printStackTrace(System.err);
                }
            }
            finally {
                handleTermination();
            }
        }

        private void handleInterrupt(final InterruptedException exception) {
            Thread.interrupted();
            try {
                if (!callDynamic("onInterrupt", new Object[]{exception})) {
                    if (!cancelled) {
                        System.err.println("The actor processing thread has been interrupted " + Thread.currentThread().getName());
                        exception.printStackTrace(System.err);
                    }
                }
            }
            finally {
                handleTermination();
            }
        }

        private boolean callDynamic(final String method, final Object[] args) {
            final List list = (List) InvokerHelper.invokeMethod(actor, "respondsTo", new Object[]{method});
            if (list != null && !list.isEmpty()) {
                InvokerHelper.invokeMethod(actor, method, args);
                return true;
            }
            return false;
        }

        public void schedule(ActorAction action) {
            if (actor.currentAction == this) {
                nextAction = action;
            } else {
                actor.getActorGroup().getThreadPool().execute(action);
            }
        }
    }

    /**
     * Buffers messages for the next continuation of an event-driven actor, handles timeouts and no-param continuations.
     *
     * @author Vaclav Pech, Alex Tkachman
     *         Date: May 22, 2009
     */
    @SuppressWarnings({"InstanceVariableOfConcreteClass"})
    private static class Reaction implements Runnable {
        private final int numberOfExpectedMessages;
        private final ActorMessage[] messages;

        private boolean timeout = false;

        private final Closure code;
        private final AbstractPooledActor actor;

        private volatile int currentIndex;
        private final static AtomicIntegerFieldUpdater<Reaction> currentSizeUpdater = AtomicIntegerFieldUpdater.newUpdater(Reaction.class, "currentSize");

        private volatile int currentSize;
        private final static AtomicIntegerFieldUpdater<Reaction> currentIndexUpdater = AtomicIntegerFieldUpdater.newUpdater(Reaction.class, "currentIndex");

        /**
         * Creates a new instance.
         *
         * @param actor                    actor
         * @param numberOfExpectedMessages The number of messages expected by the next continuation
         * @param code                     code to execute
         */
        Reaction(AbstractPooledActor actor, final int numberOfExpectedMessages, Closure code) {
            this.actor = actor;
            this.code = code;
            this.numberOfExpectedMessages = numberOfExpectedMessages;
            messages = new ActorMessage[numberOfExpectedMessages == 0 ? 1 : numberOfExpectedMessages];
        }

        @SuppressWarnings({"UnusedDeclaration"})
        Reaction(AbstractPooledActor actor, final int numberOfExpectedMessages) {
            this(actor, numberOfExpectedMessages, null);
        }

        /**
         * Retrieves the current number of messages in the buffer.
         *
         * @return The curent buffer size
         */
        public int getCurrentSize() {
            return currentSize;
        }

        /**
         * Indicates, whether a timeout message is held in the buffer
         *
         * @return True, if a timeout event has been detected.
         */
        public boolean isTimeout() {
            return timeout;
        }

        /**
         * Indicates whether the buffer contains all the messages required for the next continuation.
         *
         * @return True, if the next continuation can start.
         */
        public boolean isReady() {
            return timeout || (getCurrentSize() == (numberOfExpectedMessages == 0 ? 1 : numberOfExpectedMessages));
        }

        /**
         * Adds a new message to the buffer.
         *
         * @param message The message to add.
         */
        public void addMessage(final ActorMessage message) {
            offer(message);
        }

        /**
         * Retrieves messages for the next continuation once the MessageHolder is ready.
         *
         * @return The messages to pass to the next continuation.
         */
        public List<ActorMessage> getMessages() {
            if (!isReady()) throw new IllegalStateException("Cannot build messages before being in the ready state");
            return Collections.unmodifiableList(Arrays.asList(messages));
        }

        /**
         * Dumps so far stored messages. Useful on timeout to restore the already delivered messages
         * to the afterStop() handler in the PooledActor's sweepQueue() method..
         *
         * @return The messages stored so far.
         */
        @SuppressWarnings({"UnusedDeclaration"})
        List<ActorMessage> dumpMessages() {
            return Collections.unmodifiableList(Arrays.asList(messages));
        }

        public void run() {
            actor.runReaction(Arrays.asList(messages), numberOfExpectedMessages, code);
        }

        public void offer(ActorMessage actorMessage) {
            final int allocated = currentIndexUpdater.getAndIncrement(this);
            if (!timeout && allocated < (numberOfExpectedMessages == 0 ? 1 : numberOfExpectedMessages)) {
                messages[allocated] = actorMessage;

                if (TIMEOUT.equals(actorMessage.getPayLoad())) {
                    timeout = true;
                }

                if (timeout || currentSizeUpdater.incrementAndGet(this) == (numberOfExpectedMessages == 0 ? 1 : numberOfExpectedMessages)) {
                    if (actor != null) {
                        actor.reaction = null;
                        actor.schedule(new ActorAction(actor, this));
                        if (actor.isActorThread())
                            throw CONTINUE;
                    }
                }
            } else {
                if (actor != null) {
                    actor.messageQueue.offer(actorMessage);
                }
            }
        }

        public void setTimeout(long timeout) {
            timer.schedule(new TimerTask() {
                public void run() {
                    if (!isReady()) {
                        actor.send(new ActorMessage(TIMEOUT, null));
                    }
                }
            }, timeout);
        }

        public void checkQueue() {
            ActorMessage currentMessage;
            while (!isReady() && (currentMessage = actor.messageQueue.poll()) != null) {
                offer(currentMessage);
            }
        }
    }
}
