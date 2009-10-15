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

package groovyx.gpars;

import groovyx.gpars.actor.ActorGroup;
import groovyx.gpars.actor.Actors;
import groovyx.gpars.actor.Actor;
import groovyx.gpars.actor.ActorMessage;
import groovyx.gpars.actor.impl.*;
import static groovyx.gpars.actor.impl.ActorException.*;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.*;

import groovy.lang.GroovyRuntimeException;
import groovy.lang.Closure;
import groovy.time.Duration;
import org.codehaus.groovy.runtime.ScriptBytecodeAdapter;
import org.codehaus.groovy.runtime.GroovyCategorySupport;
import org.codehaus.groovy.runtime.CurriedClosure;

/**
 * @author Alex Tkachman
 */
public abstract class SequentialProcessingActor extends Actor implements Runnable {

    /**
     * The actor group to which the actor belongs
     */
    protected volatile ActorGroup actorGroup;

    /**
     * Code for the loop, if any
     */
    protected Runnable loopCode;

    /**
     * A copy of buffer in case of timeout.
     */
    protected List<ActorMessage> savedBufferedMessages;

    /**
     * Code for the next action
     */
    private volatile Reaction reaction;

    private volatile Node inputQueue;
    private Node outputQueue;
    private volatile int  count;

    private static final AtomicReferenceFieldUpdater<SequentialProcessingActor,Node> inputQueueUpdater = AtomicReferenceFieldUpdater.newUpdater(SequentialProcessingActor.class, Node.class, "inputQueue");

    private static final AtomicIntegerFieldUpdater<SequentialProcessingActor> countUpdater = AtomicIntegerFieldUpdater.newUpdater(SequentialProcessingActor.class, "count");

    private volatile Thread waitingThread;
    private volatile Thread currentThread;

    private static final ActorMessage startMessage      = new ActorMessage ("startMessage",null);
    private static final ActorMessage stopMessage       = new ActorMessage ("stopMessage",null);
    private static final ActorMessage loopMessage       = new ActorMessage ("loopMessage",null);
    private static final ActorMessage terminateMessage  = new ActorMessage ("terminateMessage",null);

    protected static final int S_ACTIVE_MASK         = 1;
    protected static final int S_FINISHING_MASK      = 2;
    protected static final int S_FINISHED_MASK       = 4;
    protected static final int S_STOP_TERMINATE_MASK = 8;

    protected static final int S_NOT_STARTED       = 0;
    protected static final int S_RUNNING           = S_ACTIVE_MASK;

    protected static final int S_STOPPING     = S_STOP_TERMINATE_MASK | S_FINISHING_MASK | S_ACTIVE_MASK;
    protected static final int S_TERMINATING  =                     0 | S_FINISHING_MASK | S_ACTIVE_MASK;

    protected static final int S_STOPPED      = S_STOP_TERMINATE_MASK | S_FINISHED_MASK;
    protected static final int S_TERMINATED   =                     0 | S_FINISHED_MASK;

    /**
     * Indicates whether the actor should terminate
     */
    protected volatile int stopFlag = S_NOT_STARTED;

    protected static final AtomicIntegerFieldUpdater<SequentialProcessingActor> stopFlagUpdater = AtomicIntegerFieldUpdater.newUpdater(SequentialProcessingActor.class, "stopFlag");

    /**
     * Timer holding timeouts for react methods
     */
    private static final Timer timer = new Timer(true);

    /**
     * Checks whether the current thread is the actor's current thread.
     */
    public final boolean isActorThread() {
        return Thread.currentThread() == currentThread;
    }

    /**
     * Checks the current status of the Actor.
     */
    @Override
    public final boolean isActive() {
        return (stopFlag & S_ACTIVE_MASK) != 0;
    }

    private ActorMessage getMessage() {
        assert isActorThread();

        ActorMessage toProcess;
        transferQueues();

        toProcess = outputQueue.msg;
        outputQueue = outputQueue.next;

        throwIfNeeded(toProcess);

        return toProcess;
    }

    private void throwIfNeeded(ActorMessage toProcess) {
        if (toProcess == stopMessage) {
            stopFlag = S_STOPPING;
            throw STOP;
        }

        if (toProcess == terminateMessage) {
            stopFlag = S_TERMINATING;
            throw TERMINATE;
        }
    }

    protected final ActorMessage pollMessage() {
        assert isActorThread();

        ActorMessage toProcess = null;
        transferQueues();

        if (outputQueue != null) {
            toProcess = outputQueue.msg;
            outputQueue = outputQueue.next;
        }

        return toProcess;
    }

    protected final ActorMessage takeMessage() throws InterruptedException {
        assert isActorThread();

        while (true) {
            ActorMessage toProcess = null;
            transferQueues();

            if (outputQueue != null) {
                toProcess = outputQueue.msg;
                outputQueue = outputQueue.next;

                // we are in actor thread, so counter >= 1
                // as we found message it is >= 2
                // so we have to decrement
                countUpdater.decrementAndGet(SequentialProcessingActor.this);

                throwIfNeeded(toProcess);
                return toProcess;
            }

            waitingThread = Thread.currentThread();
            LockSupport.park();
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
        }
    }

    protected ActorMessage takeMessage(long timeout, TimeUnit timeUnit) {
        assert isActorThread();

        long endTime = System.nanoTime() + timeUnit.toNanos(timeout);
        do {
            ActorMessage toProcess = null;
            transferQueues();

            if (outputQueue != null) {
                toProcess = outputQueue.msg;
                outputQueue = outputQueue.next;

                // we are in actor thread, so counter >= 1
                // as we found message it is >= 2
                // so we have to decrement
                countUpdater.decrementAndGet(SequentialProcessingActor.this);

                throwIfNeeded(toProcess);
                return toProcess;
            }

            waitingThread = Thread.currentThread();
            LockSupport.parkNanos(endTime - System.nanoTime());
        }
        while (System.nanoTime() < endTime);

        return null;
    }

    private void transferQueues() {
        if (outputQueue == null) {
            Node node = inputQueueUpdater.getAndSet(this, null);
            while (node != null) {
                Node next = node.next;
                node.next = outputQueue;
                outputQueue = node;
                node = next;
            }
        }
    }

    protected SequentialProcessingActor() {
        setActorGroup(Actors.defaultPooledActorGroup);
    }

    /**
     * Sets the actor's group.
     * It can only be invoked before the actor is started.
     *
     * @param group new group
     */
    public final void setActorGroup(final ActorGroup group) {
        if (group == null) {
            throw new IllegalArgumentException("Cannot set actor's group to null.");
        }

        if (stopFlag != S_NOT_STARTED) {
            throw new IllegalStateException("Cannot reset actor's group after it was started.");
        }

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

    public final MessageStream send(Object message) {
        int flag = stopFlag;
        if (flag != S_RUNNING) {
            if (message != terminateMessage && message != stopMessage)
                throw new IllegalStateException("The actor can not accept messages at this point.");
        }

        ActorMessage actorMessage;
        if (message instanceof ActorMessage) {
            actorMessage = (ActorMessage) message;
        } else {
            actorMessage = ActorMessage.build(message);
        }

        Node toAdd = new Node();
        toAdd.msg = actorMessage;

        int cnt = countUpdater.getAndIncrement(this);

        for (;;) {
            Node prev = inputQueue;
            toAdd.next = prev;
            if (inputQueueUpdater.compareAndSet(this, prev, toAdd)) {
                if (cnt == 0) {
                    if (flag != S_STOPPED)
                        schedule();
                }
                else {
                    Thread w = waitingThread;
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

    private void schedule() {
        actorGroup.getThreadPool().execute(this);
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

        Node node = new Node();
        node.msg = loopMessage;
        node.next = outputQueue;
        outputQueue = node;

        throw CONTINUE;
    }

    protected void handleStart() {
        doOnStart ();
    }

    protected void doOnStart() {
    }

    protected final void handleTimeout() {
        doOnTimeout();
    }

    protected void doOnTimeout() {
    }

    protected final void handleTermination() {
        Thread.interrupted();

        if (stopFlag == S_STOPPING)
            stopFlag = S_STOPPED;
        else
            if (stopFlag == S_TERMINATING)
                stopFlag = S_TERMINATED;
            else
                throw new IllegalStateException();

        try {
            doOnTermination ();
        } finally {
            if (!getJoinLatch().isBound()) {
                //noinspection unchecked
                getJoinLatch().bind(null);
            }
        }
    }

    protected void doOnTermination() {
    }

    protected final void handleException(final Throwable exception) {
        try {
            doOnException(exception);
        }
        finally {
            getJoinLatch().bind(exception);
        }
    }

    protected void doOnException(final Throwable exception) {
    }

    protected final void handleInterrupt(final InterruptedException exception) {
        Thread.interrupted();
        try {
            doOnInterrupt(exception);
        }
        finally {
            getJoinLatch().bind(exception);
        }
    }

    protected void doOnInterrupt(final InterruptedException exception) {
    }

    /**
     * Starts the Actor. No messages can be send or received before an Actor is started.
     *
     * @return this (the actor itself) to allow method chaining
     */
    @Override
    public final SequentialProcessingActor start() {
        if (!stopFlagUpdater.compareAndSet(this, S_NOT_STARTED, S_RUNNING)) {
            throw new IllegalStateException("Actor has already been started.");
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
    @Override public final Actor stop() {
        if (stopFlagUpdater.compareAndSet(this, S_RUNNING, S_STOPPING)) {
            send (stopMessage);
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
    @Override public final Actor terminate() {
        for (;;) {
            int flag = stopFlag;
            if ((flag & S_FINISHED_MASK) != 0 || flag == S_TERMINATING)
                break;

            if (stopFlagUpdater.compareAndSet(this, flag, S_TERMINATING)) {
                if (isActorThread()) {
                    throw TERMINATE;
                }

                if (currentThread != null) {
                    currentThread.interrupt();
                }
                else {
                    // just to make sure that scheduled
                    send(terminateMessage);
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

        assert reaction == null;

        final Reaction reactCode = new Reaction(this, maxNumberOfParameters, code);
        if (timeout >= 0) {
            reactCode.setTimeout(timeout);
        }
        reaction = reactCode;

        throw CONTINUE;
    }

    private static class Node {
        volatile Node next;
        ActorMessage  msg;
    }

    public void run() {
        try {
            assert currentThread == null;

            registerCurrentActorWithThread(SequentialProcessingActor.this);
            currentThread = Thread.currentThread();

            try {
                if (stopFlag == S_TERMINATING) {
                    throw TERMINATE;
                }

                ActorMessage toProcess = getMessage();

                if (toProcess == startMessage) {
                    handleStart();

                    // if we came here it means no loop wes started
                    stopFlag = S_STOPPING;
                    throw STOP;
                }

                if (toProcess == loopMessage) {
                    loopCode.run();
                    throw new IllegalStateException("Should not reach here");
                }

                if (reaction != null) {
                    reaction.offer(toProcess);
                    throw CONTINUE;
                }

                throw new IllegalStateException("Unexpected message " + toProcess);
            }
            catch (GroovyRuntimeException gre) {
                throw ScriptBytecodeAdapter.unwrap(gre);
            }
        } catch (ActorContinuationException continuation) {//
        } catch (ActorTerminationException termination) {
            handleTermination();
        } catch (ActorStopException termination) {
            handleTermination();
        } catch (ActorTimeoutException timeout) {
            stopFlag = S_TERMINATING;
            handleTimeout();
            handleTermination();
        } catch (InterruptedException e) {
            stopFlag = S_TERMINATING;
            handleInterrupt(e);
            handleTermination();
        } catch (Throwable e) {
            stopFlag = S_TERMINATING;
            handleException(e);
            handleTermination();
        } finally {
            Thread.interrupted();
            deregisterCurrentActorWithThread();

            currentThread = null;
            int cnt = countUpdater.decrementAndGet(SequentialProcessingActor.this);
            if(cnt > 0 && isActive()) {
                schedule();
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
        if (loopCode != null) {
            throw new IllegalStateException("The loop method must be only called once");
        }

        if (code instanceof Closure) {
            ((Closure) code).setResolveStrategy(Closure.DELEGATE_FIRST);
            ((Closure) code).setDelegate(this);
        }
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
        loopCode.run();
    }

    private void doLoopCall() {
        checkStopTerminate();

        if (loopCode != null) {
            scheduleLoop();
        }
        else {
            // case of react called directly from act ()
            stopFlag = S_STOPPING;
            throw STOP;
        }
    }

    protected final void checkStopTerminate() {
        if (stopFlag != S_RUNNING) {
            if (stopFlag == S_TERMINATING)
                throw TERMINATE;

            if (stopFlag != S_STOPPING)
                throw new IllegalStateException("Should not reach here");
        }
    }

    void runReaction(List<ActorMessage> messages, int maxNumberOfParameters, Closure code) {
        for (ActorMessage message : messages) {
            if (message.getPayLoad() == TIMEOUT) {
                final List<ActorMessage> saved = new ArrayList<ActorMessage>();
                for (ActorMessage m : messages) {
                    if (m != null && m.getPayLoad() != TIMEOUT) {
                        saved.add(m);
                    }
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
        GroovyCategorySupport.use(Arrays.<Class>asList(ReplyCategory.class), code);
        doLoopCall();
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
        private final SequentialProcessingActor actor;

        private int currentSize;

        /**
         * Creates a new instance.
         *
         * @param actor                    actor
         * @param numberOfExpectedMessages The number of messages expected by the next continuation
         * @param code                     code to execute
         */
        Reaction(final SequentialProcessingActor actor, final int numberOfExpectedMessages, final Closure code) {
            this.actor = actor;
            this.code = code;
            this.numberOfExpectedMessages = numberOfExpectedMessages;
            messages = new ActorMessage[numberOfExpectedMessages == 0 ? 1 : numberOfExpectedMessages];
        }

        @SuppressWarnings({"UnusedDeclaration"}) Reaction(final AbstractPooledActor actor, final int numberOfExpectedMessages) {
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
        private void addMessage(final ActorMessage message) {
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
        @SuppressWarnings({"UnusedDeclaration"}) List<ActorMessage> dumpMessages() {
            return Collections.unmodifiableList(Arrays.asList(messages));
        }

        public void run() {
            actor.runReaction(Arrays.asList(messages), numberOfExpectedMessages, code);
        }

        public void offer(final ActorMessage actorMessage) {
            if (TIMEOUT.equals(actorMessage.getPayLoad())) {
                timeout = true;
            }

            messages[currentSize++] = actorMessage;

            if (timeout || currentSize == (numberOfExpectedMessages == 0 ? 1 : numberOfExpectedMessages)) {
                actor.reaction = null;
                actor.runReaction(dumpMessages(), numberOfExpectedMessages, code);
            }
        }

        public void setTimeout(final long timeout) {
            timer.schedule(new TimerTask() {
                @Override public void run() {
                    if (!isReady()) {
                        actor.send(new ActorMessage(TIMEOUT, null));
                    }
                }
            }, timeout);
        }

        public void checkQueue() {
            ActorMessage currentMessage;
            while (!isReady() && (currentMessage = actor.pollMessage()) != null) {
                offer(currentMessage);
            }
        }
    }
}
