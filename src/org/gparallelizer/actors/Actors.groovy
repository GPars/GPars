package org.gparallelizer.actors
/**
 * Provides handy helper methods to create various types of actors and customize their behavior..
 *
 * @author Vaclav Pech
 * Date: Jan 7, 2009
 */
public class Actors {

    /**
     * Creates a new instance of DefaultActor, using the passed-in closure as the body of the actor's act() method.
     */
    public static Actor actor(Closure handler) {
        defaultActor(handler)
    }

    /**
     * Creates a new instance of DefaultActor, using the passed-in closure as the body of the actor's act() method.
     * The actor will stop after one iteration through the passed-in closure.
     */
    public static Actor oneShotActor(Closure handler) {
        defaultOneShotActor(handler)
    }

    /**
     * Creates a new instance of DefaultActor, using the passed-in closure as the body of the actor's act() method.
     */
    public static Actor defaultActor(Closure handler={throw new UnsupportedOperationException()}) {
        final Actor actor = [act: handler] as DefaultActor
        handler.delegate=actor
        return actor
    }

    /**
     * Creates a new instance of DefaultActor, using the passed-in closure as the body of the actor's act() method.
     * The actor will stop after one iteration through the passed-in closure.
     */
    public static Actor defaultOneShotActor(Closure handler={throw new UnsupportedOperationException()}) {
        Closure enhancedHandler=enhanceOneShotHandler(handler)
        final DefaultActor actor = [act: enhancedHandler] as DefaultActor
        handler.delegate=actor
        enhancedHandler.delegate=actor
        return actor
    }

    /**
     * Creates a new instance of SynchronousActor, using the passed-in closure as the body of the actor's act() method.
     */
    public static Actor synchronousActor(Closure handler={throw new UnsupportedOperationException()}) {
        final Actor actor = [act: handler] as SynchronousActor
        handler.delegate=actor
        return actor
    }

    /**
     * Creates a new instance of SynchronousActor, using the passed-in closure as the body of the actor's act() method.
     * The actor will stop after one iteration through the passed-in closure.
     */
    public static Actor synchronousOneShotActor(Closure handler={throw new UnsupportedOperationException()}) {
        Closure enhancedHandler=enhanceOneShotHandler(handler)
        final Actor actor = [act: enhancedHandler] as SynchronousActor
        handler.delegate=actor
        enhancedHandler.delegate=actor
        return actor
    }

    /**
     * Creates a new instance of BoundedActor, using the passed-in closure as the body of the actor's act() method.
     */
    public static Actor boundedActor(Closure handler={throw new UnsupportedOperationException()}) {
        return new InlinedBoundedActor(handler)
    }

    /**
     * Creates a new instance of BoundedActor, using the passed-in closure as the body of the actor's act() method.
     */
    public static Actor boundedActor(int capacity, Closure handler={throw new UnsupportedOperationException()}) {
        return new InlinedBoundedActor(capacity, handler)
    }

    /**
     * Creates a new instance of BoundedActor, using the passed-in closure as the body of the actor's act() method.
     * The actor will stop after one iteration through the passed-in closure.
     */
    public static Actor boundedOneShotActor(Closure handler={throw new UnsupportedOperationException()}) {
        Closure enhancedHandler=enhanceOneShotHandler(handler)
        Actor actor=new InlinedBoundedActor(enhancedHandler)
        handler.delegate=actor
        return actor
    }

    /**
     * Creates a new instance of BoundedActor, using the passed-in closure as the body of the actor's act() method.
     * The actor will stop after one iteration through the passed-in closure.
     */
    public static Actor boundedOneShotActor(int capacity, Closure handler={throw new UnsupportedOperationException()}) {
        Closure enhancedHandler=enhanceOneShotHandler(handler)
        Actor actor=new InlinedBoundedActor(capacity, enhancedHandler)
        handler.delegate=actor
        return actor
    }

    private static Closure enhanceOneShotHandler(Closure handler) {
        assert handler!=null
        return {handler();stop()}
    }
}

class InlinedBoundedActor extends BoundedActor {

    final Closure handler

    def InlinedBoundedActor(Closure handler) {
        this.handler = handler
        handler.delegate=this
    }

    def InlinedBoundedActor(final int capacity, Closure handler) {
        super(capacity);
        this.handler = handler
        handler.delegate=this
    }

    @Override
    protected void act() {
        handler.call()
    }
}