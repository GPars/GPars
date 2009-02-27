package org.gparallelizer.actors

/**
 * ThreadedActors are active objects, which have their own thread processing repeatedly messages submitted to them.
 * The Actor super-interface provides means to send messages to the object, start and stop the background thread as well as
 * check its status.
 *
 * @author Vaclav Pech
 * Date: Feb 20, 2009
 */
public interface ThreadedActor extends Actor {

    /**
     * Joins the actor's thread
     * @param milis Timeout in miliseconds
     */
    void join(long milis);
}