package org.gparallelizer.actors.pooledActors

import jsr166y.forkjoin.AsyncAction
import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import org.gparallelizer.actors.pooledActors.ActorContinuationException
import org.gparallelizer.actors.pooledActors.ActorTerminationException
import org.gparallelizer.actors.pooledActors.PooledActors
import static org.gparallelizer.actors.pooledActors.ActorException.TERMINATE

/**
 * Created by IntelliJ IDEA.
 * User: vaclav
 * Date: Feb 7, 2009
 */
final class ActorAction extends AsyncAction {

    private final Closure code
    private final AbstractPooledActor actor

    volatile Thread actionThread

    private def ActorAction(final AbstractPooledActor actor, final Closure code) {
        super()
        this.code = code
        this.actor = actor
        this.code.delegate = actor
    }

    protected void compute() {
        try {
            try {
                actionThread = Thread.currentThread()
                if (isCancelled()) throw TERMINATE
                code.call()
            } finally { actionThread = null }

            handleTermination()

        } catch (ActorContinuationException continuation) {
        } catch (ActorTerminationException termination) { handleTermination()
        } catch (InterruptedException e) { handleInterrupt(e)
        } catch (Exception e) { handleException(e)
        } finally {
            clearInterruptionFlag()
            actor.currentAction.compareAndSet this, null
        }
    }

    final void cancel() {
        super.cancel()
        this.actionThread?.interrupt()
    }

    private boolean clearInterruptionFlag() {
        return Thread.currentThread().interrupted()
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

    static void actorAction(AbstractPooledActor actor, Closure code) {
        final ActorAction action = new ActorAction(actor, code)
        actor.currentAction.set action
        PooledActors.pool.execute action
    }
}