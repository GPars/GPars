// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.actor;

import groovy.lang.Closure;
import groovy.time.Duration;
import groovyx.gpars.actor.impl.SequentialProcessingActor;

import java.util.concurrent.TimeUnit;

/**
 * {@code AbstractPooledActor} provides the default implementation of a stateful actor. Refer to {@code
 * DynamicDispatchActor} or {@code ReactiveActor} for examples of stateless actors.  {@code
 * AbstractPooledActor} represents a standalone active object (actor), which reacts asynchronously to
 * messages sent to it from outside through the {@code send} method, which preserving its internal implicit
 * state.  Each {@code Actor} has its own message queue and a thread pool shared with other {@code Actor}s
 * by means of an instance of the {@code PGroup}, which they have in common.  The {@code PGroup} instance is
 * responsible for the pool creation, management and shutdown.  All work performed by an {@code Actor} is
 * divided into chunks, which are sequentially submitted as independent tasks to the thread pool for
 * processing.  Whenever an {@code Actor} looks for a new message through the {@code react} method, the
 * actor gets detached from the thread, making the thread available for other actors. Thanks to the ability
 * to dynamically attach and detach threads to actors, {@code Actors} can scale far beyond the limits of the
 * underlying platform on number of concurrently available threads.  The {@code receive} method can be used
 * to read a message from the queue without giving up the thread. If no message is available, the call to
 * {@code receive} blocks until a message arrives or the supplied timeout expires.  The {@code loop} method
 * allows to repeatedly invoke a closure and yet perform each of the iterations sequentially in different
 * thread from the thread pool.  To support continuations correctly the {@code react} and {@code loop}
 * methods never return.
 * <p/>
 * <pre>
 * import static groovyx.gpars.actor.Actors.actor
 *
 * def actor = actor {
 *     loop {
 *         react { message ->
 *             println message
 *         }
 *         // This line will never be reached.
 *     }
 *     // This line will never be reached.
 * }.start()
 *
 * actor.send 'Hi!'
 * </pre>
 * <p>
 * This requires the code to be structured accordingly.
 * </p>
 * <pre>
 * def adder = actor {
 *     loop {
 *         react { a ->
 *             react { b ->
 *                 println a+b
 *                 replyIfExists a+b  // Sends reply, if b was sent by a PooledActor.
 *             }
 *         }
 *         // This line will never be reached.
 *     }
 *     // This line will never be reached.
 * }.start()
 * </pre>
 * <p>
 * The closures passed to the {@code react} method can call {@code reply} or {@code replyIfExists}, which
 * will send a message back to the originator of the currently processed message. The {@code replyIfExists}
 * method unlike the {@code reply} method will not fail if the original message wasn't sent by an actor nor
 * if the original sender actor is no longer running.  The {@code reply} and {@code replyIfExists} methods
 * are also dynamically added to the processed messages.
 * </p>
 * <pre>
 * react { a ->
 *     react { b ->
 *         reply 'message'  //sent to senders of a as well as b
 *         a.reply 'private message'  //sent to the sender of a only
 *     }
 * }
 * </pre>
 * <p>
 * The {@code react} method accepts timeouts as well.
 * </p>
 * <pre>
 * react(10, TimeUnit.MINUTES) {
 *     println 'Received message: ' + it
 * }
 * </pre>
 * <p>
 * If no message arrives within the given timeout, the {@code onTimeout} lifecycle handler is invoked, if
 * exists, and the {@code Actor.TIMEOUT} message is returned.  Each {@code Actor} has at any point in time
 * at most one active instance of {@code ActorAction} associated, which abstracts the current chunk of
 * actor's work to perform. Once a thread is assigned to the {@code ActorAction}, it moves the actor forward
 * till {@code loop} or {@code react} is called. These methods schedule another {@code ActorAction} for
 * processing and throw dedicated exception to terminate the current {@code ActorAction}.
 * </p>
 * <p>
 * Each Actor can define lifecycle observing methods, which will be called by the Actor's background thread
 * whenever a certain lifecycle event occurs.
 * </p>
 * <ul>
 * <li>{@code afterStart()} - called immediately after the {@code Actor}'s background thread has been
 * started, before the {@code act} method is called the first time.</li>
 * <li>{@code afterStop(List undeliveredMessages)} - called right after the actor is stopped, passing in all
 * the messages from the queue.</li>
 * <li>{@code onInterrupt(InterruptedException e)} - called when a {@code react} method timeouts. The actor
 * will be terminated.</li>
 * <li>{@code onTimeout()} - called when the actor's thread gets interrupted. Thread interruption will
 * result in the stopping the actor in any case.</li>
 * <li>{@code onException(Throwable e)} - called when an exception occurs in the actor's thread. Throwing an
 * exception from this method will stop the actor.</li>
 * </ul>
 *
 * @author Vaclav Pech, Alex Tkachman, Dierk Koenig
 */
@Deprecated
@SuppressWarnings({"ThrowCaughtLocally", "UnqualifiedStaticUsage"})
public abstract class AbstractPooledActor extends SequentialProcessingActor {

    private static final String THE_ACTOR_HAS_NOT_BEEN_STARTED = "The actor hasn't been started.";
    private static final String THE_ACTOR_HAS_BEEN_STOPPED = "The actor has been stopped.";
    private static final long serialVersionUID = -6232655362494852540L;
    public static final String AN_ACTOR_CAN_ONLY_RECEIVE_ONE_MESSAGE_AT_A_TIME = "An actor can only receive one message at a time";

    /**
     * Adds {@code reply} and {@code replyIfExists} methods to the current {@code Actor} and the message.
     * These methods will call {@code send} on the target actor (the sender of the original message).  The
     * {@code reply}/{@code replyIfExists} methods invoked on the actor will be sent to all currently
     * processed messages, {@code reply}/{@code replyIfExists} invoked on a message will send a reply to the
     * sender of that particular message only.
     *
     * @param message The original message
     */
    private void enhanceReplies(final ActorMessage message) {
        setSender(message == null ? null : message.getSender());
    }

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     *
     * @return The message retrieved from the queue.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    @Override
    protected final Object receiveImpl() throws InterruptedException {
        checkStoppedFlags();
        final ActorMessage message = takeMessage();
        return enhanceAndUnwrap(message);
    }

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     *
     * @param timeout how long to wait before giving up, in units of unit
     * @param units   a {@code TimeUnit} determining how to interpret the timeout parameter
     * @return The message retrieved from the queue, or null, if the timeout expires.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    @Override
    protected final Object receiveImpl(final long timeout, final TimeUnit units) throws InterruptedException {
        checkStoppedFlags();
        final ActorMessage message = takeMessage(timeout, units);
        return enhanceAndUnwrap(message);
    }

    private Object enhanceAndUnwrap(final ActorMessage message) {
        enhanceReplies(message);
        if (message == null) {
            return null;
        }
        return message.getPayLoad();
    }

    private void checkStoppedFlags() {
        if (stopFlag == S_NOT_STARTED) throw new IllegalStateException(THE_ACTOR_HAS_NOT_BEEN_STARTED);
        if (stopFlag == S_STOPPED) throw new IllegalStateException(THE_ACTOR_HAS_BEEN_STOPPED);
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

        final int maxNumberOfParameters = handler.getMaximumNumberOfParameters();
        if (maxNumberOfParameters > 1)
            throw new IllegalArgumentException(AN_ACTOR_CAN_ONLY_RECEIVE_ONE_MESSAGE_AT_A_TIME);

        checkStopTerminate();
        final ActorMessage message = takeMessage();
        enhanceReplies(message);

        try {
            if (maxNumberOfParameters == 0) {
                handler.call();
            } else {
                handler.call(message == null ? message : message.getPayLoad());
            }

        } finally {
            setSender(null);
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
        if (maxNumberOfParameters > 1)
            throw new IllegalArgumentException(AN_ACTOR_CAN_ONLY_RECEIVE_ONE_MESSAGE_AT_A_TIME);

        final long stopTime = timeUnit.toMillis(timeout) + System.currentTimeMillis();

        if (stopFlag != S_RUNNING) {
            throw new IllegalStateException(THE_ACTOR_HAS_NOT_BEEN_STARTED);
        }
        final ActorMessage message =
                takeMessage(Math.max(stopTime - System.currentTimeMillis(), 0L), TimeUnit.MILLISECONDS);

        try {
            enhanceReplies(message);

            if (maxNumberOfParameters == 0) {
                handler.call();
            } else {
                handler.call(message == null ? message : message.getPayLoad());
            }
        } finally {
            setSender(null);
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
    @SuppressWarnings({"MethodOverloadsMethodOfSuperclass", "TypeMayBeWeakened"})
    protected final void receive(final Duration duration, final Closure handler) throws InterruptedException {
        receive(duration.toMilliseconds(), TimeUnit.MILLISECONDS, handler);
    }

    @Override
    protected void handleStart() {
        super.handleStart();
        act();
    }
}
