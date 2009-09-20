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

package org.gparallelizer.actors.pooledActors;

import groovy.lang.Closure;
import groovy.lang.GroovyRuntimeException;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.runtime.ScriptBytecodeAdapter;
import org.gparallelizer.actors.ReplyRegistry;
import static org.gparallelizer.actors.pooledActors.ActorException.TERMINATE;

import java.util.List;

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
public final class ActorAction implements Runnable {

    /**
     * The code to invoke as part of this ActorAction
     */
    private final Runnable code;

    /**
     * The associated PooledActor
     */
    private final AbstractPooledActor actor;

    /**
     * The thread from the pool assigned to process the current ActorAction
     */
    private volatile Thread actionThread = null;

    /**
     * Indicates whether the cancel() method has been called
     */
    private volatile boolean cancelled = false;

    /**
     * Creates a new ActorAction asociated with a PooledActor, which will eventually perform the specified code.
     *
     * @param actor The associated PooledActor
     * @param code  The code to perform on behalf of the actor
     */
    ActorAction(final AbstractPooledActor actor, final Runnable code) {
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
            try {
                actor.getCurrentAction().set(this);

                actionThread = Thread.currentThread();
                ReplyRegistry.registerCurrentActorWithThread(this.actor);

                if (cancelled || !actor.isActive()) throw TERMINATE;
                try {
                    code.run();
                } catch (GroovyRuntimeException gre) {
                    throw ScriptBytecodeAdapter.unwrap(gre);
                }
            } finally {
                actionThread = null;
            }
            handleTermination();

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
            ReplyRegistry.deregisterCurrentActorWithThread();
            actor.getCurrentAction().compareAndSet(this, null);
        }
    }

    /**
     * Attempts to cancel the action and interrupt the thread processing it.
     */
    void cancel() {
        cancelled = true;
        if (actionThread != null)
            actionThread.interrupt();
    }

    private boolean clearInterruptionFlag() {
        return Thread.interrupted();
    }

    private void handleTimeout() {
        callDynamic("onTimeout", new Object[0]);
        handleTermination();
    }

    @SuppressWarnings({"FeatureEnvy"})
    private void handleTermination() {
        this.actor.indicateStop();
        Thread.interrupted();
        try {
            callDynamic("afterStop", new Object[]{actor.sweepQueue()});
        } finally {
            actor.releaseJoinedThreads();
        }
    }

    private void handleException(final Throwable exception) {
        if (!callDynamic("onException", new Object[]{exception})) {
            System.err.println("An exception occurred in the Actor thread ${Thread.currentThread().name}");
            exception.printStackTrace(System.err);
        }
        handleTermination();
    }

    private void handleInterrupt(final InterruptedException exception) {
        Thread.interrupted();
        if (!callDynamic("onInterrupt", new Object[]{exception})) {
            System.err.println("The actor processing thread has been interrupted ${Thread.currentThread().name}");
            exception.printStackTrace(System.err);
        }
        handleTermination();
    }

    private boolean callDynamic(final String method, final Object[] args) {
        final List list = (List) InvokerHelper.invokeMethod(actor, "respondsTo", new Object[]{method});
        if (list != null && !list.isEmpty()) {
            InvokerHelper.invokeMethod(actor, method, args);
            return true;
        }
        return false;
    }

    /**
     * Creates a new ActorAction and schedules it for processing in the thread pool belonging to the actor's group.
     *
     * @param actor actor
     * @param code  code
     */
    public static void actorAction(final AbstractPooledActor actor, final Runnable code) {
        actor.getActorGroup().getThreadPool().execute(new ActorAction(actor, code));
    }

    public Thread getActionThread() {
        return actionThread;
    }

    public void setActionThread(final Thread actionThread) {
        this.actionThread = actionThread;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(final boolean cancelled) {
        this.cancelled = cancelled;
    }
}
