package org.gparallelizer.actors.pooledActors

import jsr166y.forkjoin.AsyncAction
import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import org.gparallelizer.actors.pooledActors.ActorContinuationException
import org.gparallelizer.actors.pooledActors.ActorTerminationException
import org.gparallelizer.actors.pooledActors.PooledActors
import static org.gparallelizer.actors.pooledActors.ActorException.TERMINATE
import org.codehaus.groovy.runtime.TimeCategory

/**
 *
 * @author Vaclav Pech
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
                this.actor.currentAction.set this

                actionThread = Thread.currentThread()
                registerCurrentActorWithThread()

                if (isCancelled() || !actor.isActive()) throw TERMINATE
                use(TimeCategory) { code.call() }
            } finally { actionThread = null }

            handleTermination()

        } catch (ActorContinuationException continuation) {
        } catch (ActorTerminationException termination) { handleTermination()
        } catch (ActorTimeoutException timeout) { handleTimeout()
        } catch (InterruptedException e) { handleInterrupt(e)
        } catch (Exception e) { handleException(e)
        } finally {
            clearInterruptionFlag()
            deregisterCurrentActorWithThread()
            actor.currentAction.compareAndSet this, null
        }
    }

    private def registerCurrentActorWithThread() {
        AbstractPooledActor.currentActor.set(this.actor)
    }

    private def deregisterCurrentActorWithThread() {
        AbstractPooledActor.currentActor.set(null)
    }

    final void cancel() {
        super.cancel()
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
        PooledActors.pool.execute new ActorAction(actor, code)
    }
}