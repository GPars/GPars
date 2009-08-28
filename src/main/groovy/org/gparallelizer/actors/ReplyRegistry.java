package org.gparallelizer.actors;

/**
 * Registers and deregisters actors with their threads to allow for message-originator retrieval.
 *
 * @author Vaclav Pech
 * Date: Apr 15, 2009
 */
public abstract class ReplyRegistry {

    /**
     * Maps each thread to the actor it currently processes.
     * Used in the send() method to remember the sender of each message for potential replies
     */
    private static ThreadLocal<Actor> currentActorPerThread = new ThreadLocal<Actor>();

    /**
     * Registers the actor with the current thread
     * @param currentActor The actor to register
     */
    public static void registerCurrentActorWithThread(final Actor currentActor) {
        currentActorPerThread.set(currentActor);
    }

    /**
     * Deregisters the actor registered from the thread
     */
    public static void deregisterCurrentActorWithThread() {
        currentActorPerThread.set(null);
    }

    /**
     * Retrieves the actor registerred with the current thread
     * @return The associated actor
     */
    public static Actor threadBoundActor() {
        return currentActorPerThread.get();
    }
}
