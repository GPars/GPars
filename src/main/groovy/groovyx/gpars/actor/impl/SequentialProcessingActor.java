// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
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

package groovyx.gpars.actor.impl;

import groovy.lang.Closure;
import groovy.lang.GroovyRuntimeException;
import groovy.time.BaseDuration;
import groovy.time.Duration;
import groovyx.gpars.actor.Actor;
import groovyx.gpars.actor.ActorMessage;
import groovyx.gpars.actor.Actors;
import groovyx.gpars.group.PGroup;
import org.codehaus.groovy.runtime.CurriedClosure;
import org.codehaus.groovy.runtime.GeneratedClosure;
import org.codehaus.groovy.runtime.GroovyCategorySupport;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.runtime.ScriptBytecodeAdapter;

import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;

import static groovyx.gpars.actor.impl.ActorException.CONTINUE;
import static groovyx.gpars.actor.impl.ActorException.STOP;
import static groovyx.gpars.actor.impl.ActorException.TERMINATE;
import static groovyx.gpars.actor.impl.ActorException.TIMEOUT;

/**
 * @author Alex Tkachman, Vaclav Pech
 */
@SuppressWarnings({"UnqualifiedStaticUsage"})
public abstract class SequentialProcessingActor extends Actor implements Runnable {

    /**
     * Code for the loop, if any
     */
    protected Runnable loopCode;
    protected Closure afterLoopCode;
    protected Callable<Boolean> loopCondition;

    /**
     * Code for the next action
     */
    private volatile Reaction reaction;

    /**
     * Stored incoming messages. The most recently received message is in the head of the list.
     */
    private volatile Node inputQueue;

    /**
     * Stores messages ready for processing by the actor. The oldest message is in the head of the list.
     * Messages are transferred from the inputQueue into the output queue in the transferQueues() method.
     */
    private Node outputQueue;

    private final AtomicBoolean ongoingThreadTermination = new AtomicBoolean(false);

    /**
     * Counter of messages in the queues
     */
    private volatile int count;

    private static final AtomicReferenceFieldUpdater<SequentialProcessingActor, Node> inputQueueUpdater = AtomicReferenceFieldUpdater.newUpdater(SequentialProcessingActor.class, Node.class, "inputQueue");

    private static final AtomicIntegerFieldUpdater<SequentialProcessingActor> countUpdater = AtomicIntegerFieldUpdater.newUpdater(SequentialProcessingActor.class, "count");

    private volatile Thread waitingThread;

    private static final ActorMessage startMessage = new ActorMessage("startMessage", null);
    private static final ActorMessage loopMessage = new ActorMessage("loopMessage", null);

    protected static final int S_ACTIVE_MASK = 1;
    protected static final int S_FINISHING_MASK = 2;
    protected static final int S_FINISHED_MASK = 4;
    protected static final int S_STOP_TERMINATE_MASK = 8;

    protected static final int S_NOT_STARTED = 0;
    protected static final int S_RUNNING = S_ACTIVE_MASK;

    protected static final int S_STOPPING = S_STOP_TERMINATE_MASK | S_FINISHING_MASK | S_ACTIVE_MASK;
    protected static final int S_TERMINATING = S_FINISHING_MASK | S_ACTIVE_MASK;

    protected static final int S_STOPPED = S_STOP_TERMINATE_MASK | S_FINISHED_MASK;
    protected static final int S_TERMINATED = S_FINISHED_MASK;

    /**
     * Indicates whether the actor should terminate
     */
    protected volatile int stopFlag = S_NOT_STARTED;

    protected static final AtomicIntegerFieldUpdater<SequentialProcessingActor> stopFlagUpdater = AtomicIntegerFieldUpdater.newUpdater(SequentialProcessingActor.class, "stopFlag");

    /**
     * Timer holding timeouts for react methods
     */
    private static final Timer timer = new Timer(true);
    private static final String SHOULD_NOT_REACH_HERE = "Should not reach here";
    private static final String ERROR_EVALUATING_LOOP_CONDITION = "Error evaluating loop condition";

    /**
     * Checks the current status of the Actor.
     */
    @Override
    public final boolean isActive() {
        return (stopFlag & S_ACTIVE_MASK) != 0;
    }

    /**
     * Retrieves the next message from the queue
     *
     * @return The message
     */
    private ActorMessage getMessage() {
        assert isActorThread();

        transferQueues();

        final ActorMessage toProcess = outputQueue.msg;
        outputQueue = outputQueue.next;

        throwIfNeeded(toProcess);

        return toProcess;
    }

    /**
     * Checks the supplied message and throws either STOP or TERMINATE, if the message is a Stop or Terminate message respectively.
     *
     * @param toProcess The next message to process by the actors
     */
    private void throwIfNeeded(final ActorMessage toProcess) {
        if (toProcess == stopMessage) {
            stopFlag = S_STOPPING;
            throw STOP;
        }

        if (toProcess == terminateMessage) {
            stopFlag = S_TERMINATING;
            throw TERMINATE;
        }
    }

    /**
     * Polls a message from the queues
     *
     * @return The message
     */
    protected final ActorMessage pollMessage() {
        assert isActorThread();

        transferQueues();

        ActorMessage toProcess = null;
        if (outputQueue != null) {
            toProcess = outputQueue.msg;
            outputQueue = outputQueue.next;
        }
        return toProcess;
    }

    /**
     * Takes a message from the queues. Blocks until a message is available.
     *
     * @return The message
     * @throws InterruptedException If the thread gets interrupted.
     */
    protected final ActorMessage takeMessage() throws InterruptedException {
        assert isActorThread();

        while (true) {
            final ActorMessage message = awaitNextMessage(0L);
            if (message != null) return message;
        }
    }

    /**
     * Takes a message from the queues. Blocks until a message is available.
     *
     * @param timeout  Max time to wait for a message
     * @param timeUnit The units for the timeout
     * @return The message
     * @throws InterruptedException If the thread gets interrupted.
     */
    protected ActorMessage takeMessage(final long timeout, final TimeUnit timeUnit) throws InterruptedException {
        assert isActorThread();

        final long endTime = System.nanoTime() + timeUnit.toNanos(timeout);
        do {
            final ActorMessage message = awaitNextMessage(endTime);
            if (message != null) return message;
        } while (System.nanoTime() < endTime);

        return null;
    }

    /**
     * Holds common functionality for takeMessage() methods.
     *
     * @param endTime End of the timeout, 0 if no timeout was set
     * @return The next message
     * @throws InterruptedException If the thread has been interrupted
     */
    private ActorMessage awaitNextMessage(final long endTime) throws InterruptedException {
        transferQueues();

        waitingThread = Thread.currentThread();
        if (outputQueue != null) return retrieveNextMessage();

        if (endTime == 0L) LockSupport.park();
        else LockSupport.parkNanos(endTime - System.nanoTime());
        MessageStream.reInterrupt();
        return null;
    }

    /**
     * Takes the next message from the outputQueue, decrements the counter and possibly throws control exceptions
     *
     * @return The next message
     */
    private ActorMessage retrieveNextMessage() {
        final ActorMessage toProcess = outputQueue.msg;
        outputQueue = outputQueue.next;

        // we are in actor thread, so counter >= 1
        // as we found message it is >= 2
        // so we have to decrement
        countUpdater.decrementAndGet(this);

        throwIfNeeded(toProcess);
        return toProcess;
    }

    /**
     * Transfers messages from the input queue into the output queue, reverting the order of the elements.
     */
    private void transferQueues() {
        if (outputQueue == null) {
            Node node = inputQueueUpdater.getAndSet(this, null);
            while (node != null) {
                final Node next = node.next;
                node.next = outputQueue;
                outputQueue = node;
                node = next;
            }
        }
    }

    /**
     * Creates a new instance, sets the default actor group.
     */
    protected SequentialProcessingActor() {
        setParallelGroup(Actors.defaultActorPGroup);
    }

    /**
     * Sets the actor's group.
     * It can only be invoked before the actor is started.
     *
     * @param group new group
     */
    @Override
    public final void setParallelGroup(final PGroup group) {
        if (stopFlag != S_NOT_STARTED) {
            throw new IllegalStateException("Cannot reset actor's group after it was started.");
        }
        super.setParallelGroup(group);
        parallelGroup = group;
    }

    @Override
    public final MessageStream send(final Object message) {

        final Node toAdd = new Node(createActorMessage(message));

        while (true) {
            final Node prev = inputQueue;
            toAdd.next = prev;
            if (inputQueueUpdater.compareAndSet(this, prev, toAdd)) {
                final int cnt = countUpdater.getAndIncrement(this);

                if (cnt == 0) {
                    if (stopFlag != S_STOPPED && stopFlag != S_TERMINATED)
                        schedule();
                } else {
                    final Thread w = waitingThread;
                    if (w != null) {
                        waitingThread = null;
                        LockSupport.unpark(w);
                    }
                }
                break;
            }
        }
        return this;
    }

    @Override
    protected final boolean hasBeenStopped() {
        return stopFlag != S_RUNNING;
    }

    /**
     * Schedules the current actor for processing on the actor group's thread pool.
     */
    private void schedule() {
        parallelGroup.getThreadPool().execute(this);
    }

    protected void scheduleLoop() {
        if (stopFlag == S_TERMINATING)
            throw TERMINATE;

        transferQueues();

        if (outputQueue != null) {
            if (outputQueue.msg == stopMessage) {
                throw STOP;
            }
        }

        countUpdater.getAndIncrement(this);

        final Node node = new Node(loopMessage);
        node.next = outputQueue;
        outputQueue = node;

        throw CONTINUE;
    }

    @Override
    protected void handleTermination() {
        if (stopFlag == S_STOPPING)
            stopFlag = S_STOPPED;
        else if (stopFlag == S_TERMINATING)
            stopFlag = S_TERMINATED;
        else
            //noinspection ArithmeticOnVolatileField
            throw new IllegalStateException("Messed up actors state detected when terminating: " + stopFlag);

        try {
            super.handleTermination();
        } finally {
            getJoinLatch().bind(null);
        }
    }

    /**
     * Starts the Actor. No messages can be send or received before an Actor is started.
     *
     * @return this (the actor itself) to allow method chaining
     */
    @Override
    public final SequentialProcessingActor start() {
        if (!stopFlagUpdater.compareAndSet(this, S_NOT_STARTED, S_RUNNING)) {
            throw new IllegalStateException(ACTOR_HAS_ALREADY_BEEN_STARTED);
        }

        send(startMessage);
        return this;
    }

    /**
     * Send message to stop to the actor.
     * All messages in queue will be processed before stopped but no new messages will be accepted
     * after that point
     *
     * @return this (the actor itself) to allow method chaining
     */
    @Override
    public final Actor stop() {
        if (stopFlagUpdater.compareAndSet(this, S_RUNNING, S_STOPPING)) {
            send(stopMessage);
        }
        return this;
    }

    /**
     * Terminate the Actor. The background thread will be interrupted, unprocessed messages will be passed to the afterStop
     * method, if exists.
     * Has no effect if the Actor is not started.
     *
     * @return this (the actor itself) to allow method chaining
     */
    @Override
    public final Actor terminate() {
        while (true) {
            final int flag = stopFlag;
            if ((flag & S_FINISHED_MASK) != 0 || flag == S_TERMINATING)
                break;

            if (stopFlagUpdater.compareAndSet(this, flag, S_TERMINATING)) {
                if (isActorThread()) {
                    throw TERMINATE;
                }

                try {
                    while (!ongoingThreadTermination.compareAndSet(false, true)) //noinspection CallToThreadYield
                        Thread.yield();
                    if (currentThread != null) {
                        currentThread.interrupt();
                    } else {
                        // just to make sure that scheduled
                        try {
                            send(terminateMessage);
                        } catch (IllegalStateException ignore) {
                        }
                    }
                } finally {
                    ongoingThreadTermination.set(false);
                }

                break;
            }
        }

        return this;
    }

    /**
     * Schedules an ActorAction to take the next message off the message queue and to pass it on to the supplied closure.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     *
     * @param duration Time to wait at most for a message to arrive. The actor terminates if a message doesn't arrive within the given timeout.
     *                 The TimeCategory DSL to specify timeouts must be enabled explicitly inside the Actor's act() method.
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
        react(-1L, code);
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
    protected final void react(final long timeout, final TimeUnit timeUnit, final Closure code) {
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

        if (!isActorThread()) {
            throw new IllegalStateException("Cannot call react from thread which is not owned by the actor");
        }

        getSenders().clear();
        final int maxNumberOfParameters = code.getMaximumNumberOfParameters();

        code.setResolveStrategy(Closure.DELEGATE_FIRST);
        code.setDelegate(this);

        if (maxNumberOfParameters > 1) {
            throw new IllegalArgumentException("Actor cannot process a multi-argument closures passed to react().");
//            this.react(timeout, new MultiMessageReaction(code, maxNumberOfParameters, timeout, new ArrayList<MessageStream>()));
        } else {
            assert reaction == null;
            assert maxNumberOfParameters <= 1;

            final Reaction reactCode = new Reaction(this, maxNumberOfParameters == 1, code);
            if (timeout >= 0L) {
                reactCode.setTimeout(timeout);
            }
            reaction = reactCode;
            throw CONTINUE;
        }
    }

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     *
     * @return The message retrieved from the queue, or null, if the timeout expires.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    protected abstract Object receiveImpl() throws InterruptedException;

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     *
     * @param timeout how long to wait before giving up, in units of unit
     * @param units   a TimeUnit determining how to interpret the timeout parameter
     * @return The message retrieved from the queue, or null, if the timeout expires.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    protected abstract Object receiveImpl(final long timeout, final TimeUnit units) throws InterruptedException;

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     *
     * @return The message retrieved from the queue, or null, if the timeout expires.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    protected final Object receive() throws InterruptedException {
        final Object msg = receiveImpl();
        return SequentialProcessingActor.unwrapMessage(msg);
    }

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     *
     * @param timeout how long to wait before giving up, in units of unit
     * @param units   a TimeUnit determining how to interpret the timeout parameter
     * @return The message retrieved from the queue, or null, if the timeout expires.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    protected final Object receive(final long timeout, final TimeUnit units) throws InterruptedException {
        final Object msg = receiveImpl(timeout, units);
        return SequentialProcessingActor.unwrapMessage(msg);
    }

    private static Object unwrapMessage(final Object msg) {
        //more a double-check here, since all current implementations of the receiveImpl() method do unwrap already
        if (msg instanceof ActorMessage) {
            return ((ActorMessage) msg).getPayLoad();
        } else {
            return msg;
        }
    }

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     *
     * @param duration how long to wait before giving up, in units of unit
     * @return The message retrieved from the queue, or null, if the timeout expires.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    protected final Object receive(final BaseDuration duration) throws InterruptedException {
        return receive(duration.toMilliseconds(), TimeUnit.MILLISECONDS);
    }

    /**
     * Removes the head of the message queue
     *
     * @return The head message, or null, if the message queue is empty
     */
    @Override
    protected final ActorMessage sweepNextMessage() {
        return pollMessage();
    }

    /**
     * Enables multiple argument closures to be passed to react().
     * The MultiMessageReaction class takes just one argument and will wrap the intended closure.
     * After invoking the MultiMessageReaction will curry the obtained value onto the wrapped multi-argument closure.
     * The whole process of wrapping a multi-argument closure with MultiMessageReaction class instances is repeated until all arguments
     * are curried. At that moment the original closure, now worth all arguments curried, gets invoked.
     *
     * @author Vaclav Pech
     */
    private final class MultiMessageReaction extends Closure implements GeneratedClosure {
        private static final long serialVersionUID = -4047888721838663324L;
        private final Closure code;
        private final int maxNumberOfParameters;
        private final long timeout;
        private final List<MessageStream> localSenders;

        private MultiMessageReaction(final Closure code, final int maxNumberOfParameters, final long timeout, final List<MessageStream> localSenders) {
            super(code.getThisObject());
            this.code = code;
            this.maxNumberOfParameters = maxNumberOfParameters;
            this.timeout = timeout;
            this.localSenders = localSenders;
        }

        @Override
        public int getMaximumNumberOfParameters() {
            return 1;
        }

        @Override
        public Class[] getParameterTypes() {
            return new Class[]{Object.class};
        }

        public Object doCall(final Object args) {
            localSenders.add((MessageStream) InvokerHelper.invokeMethod(args, "getSender", null));
            final int newNumberOfParameters = maxNumberOfParameters - 1;
            if (newNumberOfParameters <= 0) {
                SequentialProcessingActor.this.getSenders().clear();
                SequentialProcessingActor.this.getSenders().addAll(localSenders);
                InvokerHelper.invokeClosure(code.curry(new Object[]{args}), null);
            } else
                SequentialProcessingActor.this.react(timeout, new MultiMessageReaction(code.curry(new Object[]{args}), newNumberOfParameters, timeout, localSenders));
            return null;
        }
    }

    /**
     * Represents an element in the message queue. Holds an ActorMessage and a reference to the next element in the queue.
     * The reference is null for the last element in the queue.
     */
    private static class Node {
        volatile Node next;
        final ActorMessage msg;

        Node(final ActorMessage actorMessage) {
            this.msg = actorMessage;
        }
    }

    @SuppressWarnings({"ThrowCaughtLocally"})
    public void run() {
        boolean shouldTerminate = false;
        //noinspection OverlyBroadCatchBlock
        try {
            assert currentThread == null;

            registerCurrentActorWithThread(this);
            currentThread = Thread.currentThread();

            try {
                if (stopFlag == S_TERMINATING) {
                    throw TERMINATE;
                }

                final ActorMessage toProcess = getMessage();

                if (toProcess == startMessage) {
                    handleStart();

                    // if we came here it means no loop was started
                    stopFlag = S_STOPPING;
                    throw STOP;
                }

                if (toProcess == loopMessage) {
                    loopCode.run();
                    throw new IllegalStateException(SHOULD_NOT_REACH_HERE);
                }

                if (reaction != null) {
                    reaction.offer(toProcess);
                    throw CONTINUE;
                }

                throw new IllegalStateException("Unexpected message " + toProcess);
            } catch (GroovyRuntimeException gre) {
                throw ScriptBytecodeAdapter.unwrap(gre);
            }

        } catch (ActorContinuationException continuation) {
            if (Thread.currentThread().isInterrupted()) {
                shouldTerminate = true;
                assert stopFlag != S_STOPPED;
                assert stopFlag != S_TERMINATED;

                stopFlag = S_TERMINATING;
                //noinspection ThrowableInstanceNeverThrown
                handleInterrupt(new InterruptedException("Interruption of the actor thread detected."));
            }

        } catch (ActorTerminationException termination) {
            shouldTerminate = true;
        } catch (ActorStopException termination) {
            assert stopFlag != S_STOPPED;
            assert stopFlag != S_TERMINATED;

            shouldTerminate = true;
        } catch (ActorTimeoutException timeout) {
            shouldTerminate = true;
            assert stopFlag != S_STOPPED;
            assert stopFlag != S_TERMINATED;

            stopFlag = S_TERMINATING;
            handleTimeout();
        } catch (InterruptedException e) {
            shouldTerminate = true;
            assert stopFlag != S_STOPPED;
            assert stopFlag != S_TERMINATED;

            stopFlag = S_TERMINATING;
            handleInterrupt(e);
        } catch (Throwable e) {
            shouldTerminate = true;
            assert stopFlag != S_STOPPED;
            assert stopFlag != S_TERMINATED;

            stopFlag = S_TERMINATING;
            handleException(e);
        } finally {
            try {
                while (!ongoingThreadTermination.compareAndSet(false, true)) //noinspection CallToThreadYield
                    Thread.yield();
                Thread.interrupted();
                if (shouldTerminate) handleTermination();
            } finally {
                deregisterCurrentActorWithThread();
                currentThread = null;
                ongoingThreadTermination.set(false);
                final int cnt = countUpdater.decrementAndGet(this);
                if (cnt > 0 && isActive()) {
                    schedule();
                }
            }
        }
    }

    /**
     * Ensures that the supplied closure will be invoked repeatedly in a loop.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     *
     * @param code The closure to invoke repeatedly
     */
    protected final void loop(final Runnable code) {
        loop((Callable<Boolean>) null, null, code);
    }

    /**
     * Ensures that the supplied closure will be invoked repeatedly in a loop.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     *
     * @param numberOfLoops The loop will only be run the given number of times
     * @param code          The closure to invoke repeatedly
     */
    protected final void loop(final int numberOfLoops, final Runnable code) {
        loop(new Callable<Boolean>() {
            private int counter = 0;

            public Boolean call() {
                counter++;
                //noinspection UnnecessaryBoxing
                return Boolean.valueOf(counter <= numberOfLoops);
            }
        }, null, code);
    }

    /**
     * Ensures that the supplied closure will be invoked repeatedly in a loop.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     *
     * @param numberOfLoops The loop will only be run the given number of times
     * @param afterLoopCode Code to run after the main actor's loop finishes
     * @param code          The closure to invoke repeatedly
     */
    protected final void loop(final int numberOfLoops, final Closure afterLoopCode, final Runnable code) {
        loop(new Callable<Boolean>() {
            private int counter = 0;

            public Boolean call() {
                counter++;
                //noinspection UnnecessaryBoxing
                return Boolean.valueOf(counter <= numberOfLoops);
            }
        }, afterLoopCode, code);
    }

    /**
     * Ensures that the supplied closure will be invoked repeatedly in a loop.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     *
     * @param condition A condition to evaluate before each iteration starts. If the condition returns false, the loop exits.
     * @param code      The closure to invoke repeatedly
     */
    protected final void loop(final Closure condition, final Runnable code) {
        loop(new Callable<Boolean>() {
            public Boolean call() {
                return (Boolean) condition.call();
            }
        }, null, code);

    }

    /**
     * Ensures that the supplied closure will be invoked repeatedly in a loop.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     *
     * @param condition     A condition to evaluate before each iteration starts. If the condition returns false, the loop exits.
     * @param afterLoopCode Code to run after the main actor's loop finishes
     * @param code          The closure to invoke repeatedly
     */
    protected final void loop(final Closure condition, final Closure afterLoopCode, final Runnable code) {
        loop(new Callable<Boolean>() {
            public Boolean call() {
                return (Boolean) condition.call();
            }
        }, afterLoopCode, code);

    }

    /**
     * Ensures that the supplied closure will be invoked repeatedly in a loop.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     *
     * @param condition     A condition to evaluate before each iteration starts. If the condition returns false, the loop exits.
     * @param afterLoopCode Code to run after the main actor's loop finishes
     * @param code          The closure to invoke repeatedly
     */
    private void loop(final Callable<Boolean> condition, final Closure afterLoopCode, final Runnable code) {
        if (loopCode != null || loopCondition != null) {
            throw new IllegalStateException("The loop method must be only called once");
        }

        if (code instanceof Closure) {
            ((Closure) code).setResolveStrategy(Closure.DELEGATE_FIRST);
            ((Closure) code).setDelegate(this);
        }
        loopCondition = condition;
        loopCode = new Runnable() {
            public void run() {
                getSenders().clear();
                obj2Sender.clear();

                if (code instanceof Closure)
                //noinspection deprecation
                {
                    GroovyCategorySupport.use(Arrays.<Class>asList(ReplyCategory.class), (Closure) code);
                } else {
                    code.run();
                }
                doLoopCall();
            }
        };
        if (afterLoopCode != null) {
            this.afterLoopCode = afterLoopCode;
            this.afterLoopCode.setDelegate(this);
            this.afterLoopCode.setResolveStrategy(Closure.DELEGATE_FIRST);
        }
        doLoopCall();
    }

    private static boolean verifyLoopCondition(final Callable<Boolean> condition) {
        try {
            if (condition == null) return true;
            return condition.call() == Boolean.TRUE;
        } catch (Exception e) {
            throw new RuntimeException(ERROR_EVALUATING_LOOP_CONDITION, e);
        }
    }

    private void doLoopCall() {
        if (verifyLoopCondition(loopCondition)) {
            checkStopTerminate();

            //noinspection VariableNotUsedInsideIf
            if (loopCode != null) {
                scheduleLoop();  //throws a control exception
            }
        }
        if (afterLoopCode != null) {
            loopCode = null;
            final Closure localAfterLoopCode = afterLoopCode;
            afterLoopCode = null;
            localAfterLoopCode.call();
        }
        stopFlag = S_STOPPING;
        throw STOP;
    }

    final void runReaction(final ActorMessage message, final Closure code) {
        runEnhancedWithReplies(message, code);
        doLoopCall();
    }

    protected final void checkStopTerminate() {
        if (hasBeenStopped()) {
            if (stopFlag == S_TERMINATING)
                throw TERMINATE;

            if (stopFlag != S_STOPPING)
                throw new IllegalStateException(SHOULD_NOT_REACH_HERE);
        }
    }

    /**
     * Buffers messages for the next continuation of an event-driven actor, handles timeouts and no-param continuations.
     *
     * @author Vaclav Pech, Alex Tkachman
     *         Date: May 22, 2009
     */
    @SuppressWarnings({"InstanceVariableOfConcreteClass"})
    private static final class Reaction {
        private final boolean codeNeedsArgument;
        private final AtomicBoolean isReady = new AtomicBoolean(false);
        private final Closure code;
        private final SequentialProcessingActor actor;

        /**
         * Creates a new instance.
         *
         * @param actor             actor
         * @param codeNeedsArgument Indicates, whether the provided code expects an argument
         * @param code              code to execute
         */
        Reaction(final SequentialProcessingActor actor, final boolean codeNeedsArgument, final Closure code) {
            this.actor = actor;
            this.code = code;
            this.codeNeedsArgument = codeNeedsArgument;
        }

        /**
         * Indicates whether a message or a timeout has arrived.
         *
         * @return True, if the next continuation can start.
         */
        @SuppressWarnings({"BooleanMethodIsAlwaysInverted"})
        public boolean isReady() {
            return isReady.get();
        }

        public void offer(final ActorMessage actorMessage) {
            final boolean readyFlag = isReady.getAndSet(true);
            assert !readyFlag;

            actor.reaction = null;

            if (codeNeedsArgument) {
                actor.runReaction(actorMessage, new CurriedClosure(code, new Object[]{actorMessage.getPayLoad()}));
            } else {
                actor.runReaction(actorMessage, code);
            }
        }

        public void setTimeout(final long timeout) {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (!isReady()) {
                        actor.send(new ActorMessage(TIMEOUT, null));
                    }
                }
            }, timeout);
        }
    }
}
