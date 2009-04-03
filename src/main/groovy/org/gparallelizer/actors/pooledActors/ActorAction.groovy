package org.gparallelizer.actors.pooledActors

import org.codehaus.groovy.runtime.TimeCategory
import org.gparallelizer.actors.pooledActors.*
import static org.gparallelizer.actors.pooledActors.ActorException.TERMINATE

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
     * Lock to prevent cancellation after the thread gets detached from the action
     */
    private final Object actionThreadCancellationLock = new Object()

    /**
     * Maps each thread to the actor it currently processes.
     * Used in the send() method to remember the sender of each message for potential replies
     */
    static ThreadLocal<PooledActor> currentActorPerThread = new ThreadLocal<PooledActor>()

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
    protected void compute() {
        try {
            try {
                this.actor.currentAction.set this

                actionThread = Thread.currentThread()
                registerCurrentActorWithThread()

                if (cancelled || !actor.isActive()) throw TERMINATE
                use(TimeCategory) { code.call() }
            } finally {
                synchronized (actionThreadCancellationLock) {
                    actionThread = null
                }
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
            deregisterCurrentActorWithThread()
            actor.currentAction.compareAndSet this, null
        }
    }

    private def registerCurrentActorWithThread() {
        currentActorPerThread.set(this.actor)
    }

    private def deregisterCurrentActorWithThread() {
        currentActorPerThread.set(null)
    }

    /**
     * Attempts to cancel the action and interrupt the thread processing it.
     */
    final void cancel() {
        synchronized(actionThreadCancellationLock) {
            cancelled = true
            this.actionThread?.interrupt()
        }
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
        if (actor.respondsTo('afterStop')) actor.afterStop(actor.sweepQueue())
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
     * Creates a new ActorAction and schedules it for processing.
     */
    static void actorAction(AbstractPooledActor actor, Closure code) {
        PooledActors.pool.execute new ActorAction(actor, code)
    }

    /**
     * Performs the ActorAction
     */
    public void run() {
        compute()
    }
}