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

import org.codehaus.groovy.runtime.TimeCategory
import org.gparallelizer.actors.pooledActors.*
import static org.gparallelizer.actors.pooledActors.ActorException.TERMINATE
import org.gparallelizer.actors.ReplyRegistry

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
 * @author Vaclav Pech
 * Date: Feb 7, 2009
 */
final class ActorAction implements Runnable {

    /**
     * The code to invoke as part of this ActorAction
     */
    private final Closure code

    /**
     * The associated PooledActor
     */
    private final AbstractPooledActor actor

    /**
     * The thread from the pool assigned to process the current ActorAction
     */
    volatile Thread actionThread

    /**
     * Indicates whether the cancel() method has been called
     */
    volatile boolean cancelled = false

    /**
     * Creates a new ActorAction asociated with a PooledActor, which will eventually perform the specified code.
     * @param actor The associated PooledActor
     * @param code The code to perform on behalf of the actor
     */
    private def ActorAction(final AbstractPooledActor actor, final Closure code) {
        super()
        this.code = code
        this.actor = actor
        this.code.delegate = actor
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
                this.actor.currentAction.set this

                actionThread = Thread.currentThread()
                ReplyRegistry.registerCurrentActorWithThread(this.actor)

                if (cancelled || !actor.isActive()) throw TERMINATE
                use(TimeCategory) { code.call() }
            } finally {
                actionThread = null
            }
            handleTermination()

        } catch (ActorContinuationException continuation) {
        } catch (ActorTerminationException termination) {
            handleTermination()
        } catch (ActorTimeoutException timeout) {
            handleTimeout()
        } catch (InterruptedException e) {
            handleInterrupt(e)
        } catch (Exception e) {
            handleException(e)
        } finally {
            clearInterruptionFlag()
            ReplyRegistry.deregisterCurrentActorWithThread()
            actor.currentAction.compareAndSet this, null
        }
    }

    /**
     * Attempts to cancel the action and interrupt the thread processing it.
     */
    final void cancel() {
        cancelled = true
        this.actionThread?.interrupt()
    }

    private boolean clearInterruptionFlag() {
        return Thread.currentThread().interrupted()
    }

    private def handleTimeout() {
        if (actor.respondsTo('onTimeout')) actor.onTimeout()
        handleTermination()
    }

    private def handleTermination() {
        this.actor.indicateStop()
        Thread.currentThread().interrupted()
        try {
            final List queuedMessages = actor.sweepQueue()
            if (actor.respondsTo('afterStop')) actor.afterStop(queuedMessages)
        } finally {
            actor.releaseJoinedThreads()
        }
    }

    private def handleException(final Exception exception) {
        if (actor.respondsTo('onException')) actor.onException(exception)
        else {
            System.err.println("An exception occured in the Actor thread ${Thread.currentThread().name}")
            exception.printStackTrace(System.err)
        }
        handleTermination()
    }

    private def handleInterrupt(final InterruptedException exception) {
        clearInterruptionFlag()
        if (actor.respondsTo('onInterrupt')) actor.onInterrupt(exception)
        else {
            System.err.println("The actor processing thread has been interrupted ${Thread.currentThread().name}")
            exception.printStackTrace(System.err)
        }
        handleTermination()
    }

    /**
     * Creates a new ActorAction and schedules it for processing in the thread pool belonging to the actor's group.
     */
    static void actorAction(AbstractPooledActor actor, Closure code) {
        actor.actorGroup.execute new ActorAction(actor, code)
    }
}
