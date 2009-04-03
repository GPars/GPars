package org.gparallelizer.actors.pooledActors

import org.gparallelizer.actors.Actor

/**
 * PooledActors are active objects, which do not have their own thread but borrow a thread from a thread pool
 * for repeatedly processing messages submitted to them. 
 * The Actor interface provides means to send messages to the actor, start and stop the background thread as well as
 * check its status.
 *
 * @author Vaclav Pech
 * Date: Feb 20, 2009
 */
public interface PooledActor extends Actor {}