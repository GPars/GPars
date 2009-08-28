package org.gparallelizer.actors.pooledActors;

/**
 * An exception indicating end of a work chunk (ActorAction) allowing other ActorAction to get scheduled.
 * Implementing singleton pattern, ActorException holds the unique reference.
 *
 * @author Vaclav Pech
 * Date: Feb 17, 2009
 */
final class ActorContinuationException extends ActorException {

    ActorContinuationException() { }
}
