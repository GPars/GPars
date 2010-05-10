// GPars (formerly GParallelizer)
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

package groovyx.gpars.agent;

import groovyx.gpars.actor.Actors;
import groovyx.gpars.group.PGroup;
import groovyx.gpars.scheduler.Pool;
import org.codehaus.groovy.runtime.NullObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * @author Vaclav Pech
 *         Date: 13.4.2010
 */
public abstract class AgentCore implements Runnable {

    /**
     * The thread pool to use with this agent
     */
    private volatile Pool threadPool = Actors.defaultActorPGroup.getThreadPool();

    /**
     * Retrieves the thread pool used by the agent
     *
     * @return The thread pool
     */
    public final Pool getThreadPool() {
        return threadPool;
    }

    /**
     * Sets a new thread pool to be used by the agent
     *
     * @param threadPool The thread pool to use
     */
    public final void attachToThreadPool(final Pool threadPool) {
        this.threadPool = threadPool;
    }

    /**
     * Sets an actor group to use for task scheduling
     *
     * @param pGroup The pGroup to use
     */
    public void setPGroup(final PGroup pGroup) {
        attachToThreadPool(pGroup.getThreadPool());
    }

    /**
     * Fair agents give up the thread after processing each message, non-fair agents keep a thread until their message queue is empty.
     */
    private volatile boolean fair = false;

    /**
     * Retrieves the agent's fairness flag
     * Fair agents give up the thread after processing each message, non-fair agents keep a thread until their message queue is empty.
     * Non-fair agents tends to perform better than fair ones.
     *
     * @return True for fair agents, false for non-fair ones. Agents are non-fair by default.
     */
    public boolean isFair() {
        return fair;
    }

    /**
     * Makes the agent fair. Agents are non-fair by default.
     * Fair agents give up the thread after processing each message, non-fair agents keep a thread until their message queue is empty.
     * Non-fair agents tends to perform better than fair ones.
     */
    public void makeFair() {
        this.fair = true;
    }

    /**
     * Holds agent errors
     */
    private List<Exception> errors;

    /**
     * Incoming messages
     */
    private final Queue<Object> queue = new ConcurrentLinkedQueue<Object>();

    /**
     * Indicates, whether there's an active thread handling a message inside the agent's body
     */
    private volatile int active = PASSIVE;
    private static final AtomicIntegerFieldUpdater<AgentCore> activeUpdater = AtomicIntegerFieldUpdater.newUpdater(AgentCore.class, "active");
    private static final int PASSIVE = 0;
    private static final int ACTIVE = 1;

    /**
     * Adds the message to the agent\s message queue
     *
     * @param message A value or a closure
     */
    public final void send(final Object message) {
        queue.add(message != null ? message : NullObject.getNullObject());
        schedule();
    }

    /**
     * Adds the message to the agent\s message queue
     *
     * @param message A value or a closure
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public final void leftShift(final Object message) {
        send(message);
    }

    /**
     * Adds the message to the agent\s message queue
     *
     * @param message A value or a closure
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public final void call(final Object message) {
        send(message);
    }

    /**
     * Dynamically dispatches the method call
     *
     * @param message A value or a closure
     */
    abstract void handleMessage(final Object message);

    /**
     * Schedules processing of a next message, if there are some and if there isn't an active thread handling a message at the moment
     */
    void schedule() {
        if (!queue.isEmpty() && activeUpdater.compareAndSet(this, PASSIVE, ACTIVE)) {
            threadPool.execute(this);
        }
    }

    /**
     * Handles a single message from the message queue
     */
    @SuppressWarnings({"CatchGenericClass"})
    public void run() {
        try {
            Object message = queue.poll();
            while (message != null) {
                this.handleMessage(message);
                if (fair) break;
                message = queue.poll();
            }
        } catch (Exception e) {
            registerError(e);
        } finally {
            activeUpdater.set(this, PASSIVE);
            schedule();
        }
    }

    /**
     * Adds the exception to the list of thrown exceptions
     *
     * @param e The exception to store
     */
    @SuppressWarnings({"MethodOnlyUsedFromInnerClass", "SynchronizedMethod"})
    private synchronized void registerError(final Exception e) {
        if (errors == null) errors = new ArrayList<Exception>();
        errors.add(e);
    }

    /**
     * Retrieves a list of exception thrown within the agent's body.
     * Clears the exception history
     *
     * @return A detached collection of exception that have occurred in the agent's body
     */
    @SuppressWarnings({"SynchronizedMethod", "ReturnOfCollectionOrArrayField"})
    public synchronized List<Exception> getErrors() {
        if (errors == null) return Collections.emptyList();
        try {
            return errors;
        } finally {
            errors = null;
        }
    }
}
