package org.gparallelizer.actors;

/**
 *
 * @author Vaclav Pech
 * Date: Apr 15, 2009
 */
public class ReplyRegistry {

    /**
     * Maps each thread to the actor it currently processes.
     * Used in the send() method to remember the sender of each message for potential replies
     */
    private static ThreadLocal<Actor> currentActorPerThread = new ThreadLocal<Actor>();

    public static void registerCurrentActorWithThread(final Actor currentActor) {
        currentActorPerThread.set(currentActor);
    }

    public static void deregisterCurrentActorWithThread() {
        currentActorPerThread.set(null);
    }

    public static Actor threadBoundActor() {
        return currentActorPerThread.get();
    }
    //todo comment
}
