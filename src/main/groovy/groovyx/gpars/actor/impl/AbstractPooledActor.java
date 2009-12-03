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

package groovyx.gpars.actor.impl;

import groovy.lang.Closure;
import groovy.lang.MetaClass;
import groovy.time.Duration;
import groovyx.gpars.MessageStream;
import groovyx.gpars.SequentialProcessingActor;
import groovyx.gpars.actor.ActorMessage;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AbstractPooledActor provides the default Actor implementation. It represents a standalone active object (actor),
 * which reacts asynchronously to messages sent to it from outside through the send() method.
 * Each Actor has its own message queue and a thread pool shared with other Actors by means of an instance
 * of the ActorGroup, which they have in common.
 * The ActorGroup instance is responsible for the pool creation, management and shutdown.
 * All work performed by an Actor is divided into chunks, which are sequentially submitted as independent tasks
 * to the thread pool for processing.
 * Whenever an Actor looks for a new message through the react() method, the actor gets detached
 * from the thread, making the thread available for other actors. Thanks to the ability to dynamically attach and detach
 * threads to actors, Actors can scale far beyond the limits of the underlying platform on number of concurrently
 * available threads.
 * The receive() method can be used to read a message from the queue without giving up the thread. If no message is available,
 * the call to receive() blocks until a message arrives or the supplied timeout expires.
 * The loop() method allows to repeatedly invoke a closure and yet perform each of the iterations sequentially
 * in different thread from the thread pool.
 * To support continuations correctly the react() and loop() methods never return.
 * <pre>
 * import static groovyx.gpars.actor.Actors.actor
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
 * <p/>
 * The react() method accepts timeout specified using the TimeCategory DSL.
 * <pre>
 * react(10.MINUTES) {
 *     println 'Received message: ' + it
 * }
 * </pre>
 * If no message arrives within the given timeout, the onTimeout() lifecycle handler is invoked, if exists,
 * and the actor terminates.
 * Each Actor has at any point in time at most one active instance of ActorAction associated, which abstracts
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
@SuppressWarnings({"ThrowCaughtLocally", "UnqualifiedStaticUsage"})
public abstract class AbstractPooledActor extends SequentialProcessingActor {

    private volatile Closure onStop = null;
    private static final String THE_ACTOR_HASN_T_BEEN_STARTED = "The actor hasn't been started.";
    private static final String THE_ACTOR_HAS_BEEN_STOPPED = "The actor has been stopped.";
    private static final String RESPONDS_TO = "respondsTo";
    private static final String ON_DELIVERY_ERROR = "onDeliveryError";
    private static final Object[] EMPTY_ARGUMENTS = new Object[0];
    private static final String AFTER_START = "afterStart";

    protected AbstractPooledActor() {
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
     * Adds reply() and replyIfExists() methods to the currentActor and the message.
     * These methods will call send() on the target actor (the sender of the original message).
     * The reply()/replyIfExists() methods invoked on the actor will be sent to all currently processed messages,
     * reply()/replyIfExists() invoked on a message will send a reply to the sender of that particular message only.
     *
     * @param messages List of ActorMessage wrapping the sender actor, who we need to be able to respond to,
     *                 plus the original message
     */
    private void enhanceReplies(final Iterable<ActorMessage> messages) {
        final List<MessageStream> senders = getSenders();
        senders.clear();
        for (final ActorMessage message : messages) {
            senders.add(message == null ? null : message.getSender());
            if (message != null) {
                obj2Sender.put(message.getPayLoad(), message.getSender());
            }
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
        if (stopFlag == S_NOT_STARTED) {
            throw new IllegalStateException(THE_ACTOR_HASN_T_BEEN_STARTED);
        }

        if (stopFlag == S_STOPPED) {
            throw new IllegalStateException(THE_ACTOR_HAS_BEEN_STOPPED);
        }

        final ActorMessage message = takeMessage();
        enhanceReplies(Arrays.asList(message));
        if (message == null) {
            return null;
        }
        return message.getPayLoad();
    }

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     *
     * @param timeout  how long to wait before giving up, in units of unit
     * @param units a TimeUnit determining how to interpret the timeout parameter
     * @return The message retrieved from the queue, or null, if the timeout expires.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    @Override
    protected final Object receiveImpl(final long timeout, final TimeUnit units) throws InterruptedException {
        if (stopFlag == S_NOT_STARTED) {
            throw new IllegalStateException(THE_ACTOR_HASN_T_BEEN_STARTED);
        }

        if (stopFlag == S_STOPPED) {
            throw new IllegalStateException(THE_ACTOR_HAS_BEEN_STOPPED);
        }

        final ActorMessage message = takeMessage(timeout, units);
        enhanceReplies(Arrays.asList(message));
        if (message == null) {
            return null;
        }
        return message.getPayLoad();
    }

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     * The message retrieved from the queue is passed into the handler as the only parameter.
     *
     * @param handler A closure accepting the retrieved message as a parameter, which will be invoked after a message is received.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    @SuppressWarnings({"MethodOverloadsMethodOfSuperclass"})
    protected final void receive(final Closure handler) throws InterruptedException {
        handler.setResolveStrategy(Closure.DELEGATE_FIRST);
        handler.setDelegate(this);

        final List<ActorMessage> messages = new ArrayList<ActorMessage>();
        final int maxNumberOfParameters = handler.getMaximumNumberOfParameters();
        final int toReceive = maxNumberOfParameters == 0 ? 1 : maxNumberOfParameters;

        collectRequiredMessages(messages, toReceive);
        enhanceReplies(messages);

        try {
            if (maxNumberOfParameters == 0) {
                handler.call();
            } else {
                final Object[] args = new Object[messages.size()];
                for (int i = 0; i < args.length; i++) {
                    args[i] = messages.get(i).getPayLoad();
                }
                handler.call(args);
            }

        } finally {
            getSenders().clear();
        }
    }

    private void collectRequiredMessages(final Collection<ActorMessage> messages, final int toReceive) throws InterruptedException {
        for (int i = 0; i != toReceive; ++i) {
            checkStopTerminate();
            messages.add(takeMessage());
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
    protected final void receive(final long timeout, final TimeUnit timeUnit, final Closure handler) throws InterruptedException {
        handler.setResolveStrategy(Closure.DELEGATE_FIRST);
        handler.setDelegate(this);

        final int maxNumberOfParameters = handler.getMaximumNumberOfParameters();
        final int toReceive = maxNumberOfParameters == 0 ? 1 : maxNumberOfParameters;

        final long stopTime = timeUnit.toMillis(timeout) + System.currentTimeMillis();

        boolean nullAppeared = false;  //Ignore further potential messages once a null is retrieved (due to a timeout)
        final List<ActorMessage> messages = new ArrayList<ActorMessage>();
        for (int i = 0; i != toReceive; ++i) {
            if (nullAppeared) {
                messages.add(null);
            } else {
                if (stopFlag != S_RUNNING) {
                    throw new IllegalStateException(THE_ACTOR_HASN_T_BEEN_STARTED);
                }
                final ActorMessage message =
                        takeMessage(Math.max(stopTime - System.currentTimeMillis(), 0L), TimeUnit.MILLISECONDS);
                nullAppeared = message == null;
                messages.add(message);
            }
        }

        try {
            enhanceReplies(messages);

            if (maxNumberOfParameters == 0) {
                handler.call();
            } else {
                final Object[] args = retrievePayloadOfMessages(messages);
                handler.call(args);
            }
        }
        finally {
            getSenders().clear();
        }
    }

    private static Object[] retrievePayloadOfMessages(final List<ActorMessage> messages) {
        final Object[] args = new Object[messages.size()];
        for (int i = 0; i < args.length; i++) {
            final ActorMessage am = messages.get(i);
            args[i] = am == null ? am : am.getPayLoad();
        }
        return args;
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
    @SuppressWarnings({"MethodOverloadsMethodOfSuperclass", "TypeMayBeWeakened"})
    protected final void receive(final Duration duration, final Closure handler) throws InterruptedException {
        receive(duration.toMilliseconds(), TimeUnit.MILLISECONDS, handler);
    }

    /**
     * Clears the message queue returning all the messages it held.
     *
     * @return The messages stored in the queue
     */
    final List sweepQueue() {
        final List<ActorMessage> messages = new ArrayList<ActorMessage>();

        ActorMessage message = pollMessage();
        while (message != null) {
            Object list = InvokerHelper.invokeMethod(message.getPayLoad(), RESPONDS_TO, new Object[]{ON_DELIVERY_ERROR});
            if (list != null && !((Collection) list).isEmpty()) {
                InvokerHelper.invokeMethod(message.getPayLoad(), ON_DELIVERY_ERROR, EMPTY_ARGUMENTS);
            } else {
                list = InvokerHelper.invokeMethod(message.getSender(), RESPONDS_TO, new Object[]{ON_DELIVERY_ERROR});
                if (list != null && !((Collection) list).isEmpty()) {
                    InvokerHelper.invokeMethod(message.getSender(), ON_DELIVERY_ERROR, EMPTY_ARGUMENTS);
                }
            }

            messages.add(message);
            message = pollMessage();
        }
        return messages;
    }

    /**
     * Set on stop handler for this actor
     *
     * @param onStop The code to invoke when stopping
     */
    public final void onStop(final Closure onStop) {
        if (onStop != null) {
            this.onStop = (Closure) onStop.clone();
            this.onStop.setDelegate(this);
            this.onStop.setResolveStrategy(Closure.DELEGATE_FIRST);
        }
    }

    @Override
    protected void doOnStart() {
        final Object list = InvokerHelper.invokeMethod(this, RESPONDS_TO, new Object[]{AFTER_START});
        if (list != null && !((Collection) list).isEmpty()) {
            InvokerHelper.invokeMethod(this, AFTER_START, EMPTY_ARGUMENTS);
        }
        act();
    }

    @Override
    protected void doOnTimeout() {
        callDynamic("onTimeout", EMPTY_ARGUMENTS);
    }

    @Override
    @SuppressWarnings({"FeatureEnvy"})
    protected void doOnTermination() {
        final List queue = sweepQueue();
        if (onStop != null)
            onStop.call(queue);

        callDynamic("afterStop", new Object[]{queue});
    }

    @SuppressWarnings({"UseOfSystemOutOrSystemErr"})
    @Override
    protected void doOnException(final Throwable exception) {
        if (!callDynamic("onException", new Object[]{exception})) {
            System.err.println("An exception occurred in the Actor thread " + Thread.currentThread().getName());
            exception.printStackTrace(System.err);
        }
    }

    @SuppressWarnings({"UseOfSystemOutOrSystemErr"})
    @Override
    protected void doOnInterrupt(final InterruptedException exception) {
        if (!callDynamic("onInterrupt", new Object[]{exception})) {
            if (stopFlag == S_RUNNING) {
                System.err.println("The actor processing thread has been interrupted " + Thread.currentThread().getName());
                exception.printStackTrace(System.err);
            }
        }
    }

    private boolean callDynamic(final String method, final Object[] args) {
        final MetaClass metaClass = InvokerHelper.getMetaClass(this);
        final List list = metaClass.respondsTo(this, method);
        if (list != null && !list.isEmpty()) {
            InvokerHelper.invokeMethod(this, method, args);
            return true;
        }
        return false;
    }

}
