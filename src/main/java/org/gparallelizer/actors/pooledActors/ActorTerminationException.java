package org.gparallelizer.actors.pooledActors;

/**
 * An exception indicating Actor termination request.
 * Implementing singleton pattern, ActorException holds the unique reference.
 *
 * @author Vaclav Pech
 * Date: Feb 17, 2009
 */
final class ActorTerminationException extends ActorException {

    ActorTerminationException() { }
}