package org.gparallelizer.actors
/**
 * Provides handy helper methods to create various types of actors and customize their behavior..
 * All actors created through the Actors class belong to the same ActorGroup - Actors.defaultActorGroup.
 * If you need to customize actor grouping, use ActorGroup class directly instead.
 *
 * @author Vaclav Pech
 * Date: Jan 7, 2009
 */
public class Actors {

    /**
     * The default actor group to share by all actors created through the Actors class.
     */
    public final static ActorGroup defaultActorGroup = new ActorGroup(false)

    /**
     * Creates a new instance of DefaultActor, using the passed-in closure as the body of the actor's act() method.
     */
    public static Actor actor(Closure handler) {
        defaultActorGroup.actor handler
    }

    /**
     * Creates a new instance of DefaultActor, using the passed-in closure as the body of the actor's act() method.
     * The actor will stop after one iteration through the passed-in closure.
     */
    public static Actor oneShotActor(Closure handler) {
        defaultActorGroup.defaultOneShotActor handler
    }

    /**
     * Creates a new instance of DefaultActor, using the passed-in closure as the body of the actor's act() method.
     */
    public static Actor defaultActor(Closure handler) {
        defaultActorGroup.defaultActor handler
    }

    /**
     * Creates a new instance of DefaultActor, using the passed-in closure as the body of the actor's act() method.
     * The actor will stop after one iteration through the passed-in closure.
     */
    public static Actor defaultOneShotActor(Closure handler) {
        defaultActorGroup.defaultOneShotActor handler
    }

    /**
     * Creates a new instance of SynchronousActor, using the passed-in closure as the body of the actor's act() method.
     */
    public static Actor synchronousActor(Closure handler) {
        defaultActorGroup.synchronousActor handler
    }

    /**
     * Creates a new instance of SynchronousActor, using the passed-in closure as the body of the actor's act() method.
     * The actor will stop after one iteration through the passed-in closure.
     */
    public static Actor synchronousOneShotActor(Closure handler) {
        defaultActorGroup.synchronousOneShotActor handler
    }

    /**
     * Creates a new instance of BoundedActor, using the passed-in closure as the body of the actor's act() method.
     */
    public static Actor boundedActor(Closure handler) {
        defaultActorGroup.boundedActor handler
    }

    /**
     * Creates a new instance of BoundedActor, using the passed-in closure as the body of the actor's act() method.
     */
    public static Actor boundedActor(int capacity, Closure handler) {
        defaultActorGroup.boundedActor capacity, handler
    }

    /**
     * Creates a new instance of BoundedActor, using the passed-in closure as the body of the actor's act() method.
     * The actor will stop after one iteration through the passed-in closure.
     */
    public static Actor boundedOneShotActor(Closure handler) {
        defaultActorGroup.boundedOneShotActor handler
    }

    /**
     * Creates a new instance of BoundedActor, using the passed-in closure as the body of the actor's act() method.
     * The actor will stop after one iteration through the passed-in closure.
     */
    public static Actor boundedOneShotActor(int capacity, Closure handler) {
        defaultActorGroup.boundedOneShotActor capacity, handler
    }
}
