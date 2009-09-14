//  GParallelizer
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License. 

package org.gparallelizer.actors

/**
 * Provides a common super class fo thread-actor's groups.
 *
 * @author Vaclav Pech
 * Date: May 8, 2009
 */
public abstract class AbstractThreadActorGroup extends AbstractActorGroup {

    /**
     * Creates a group of actors. The actors will share a common thread pool of threads.
     */
    protected def AbstractThreadActorGroup() {  }

    /**
     * Creates a group of actors. The actors will share a common thread pool.
     * @param useForkJoinPool Indicates, whether the group should use a fork join pool underneath or the executor-service-based default pool
     */
    protected def AbstractThreadActorGroup(final boolean useForkJoinPool) { super(useForkJoinPool) }

    /**
     * Creates a new instance of DefaultThreadActor, using the passed-in closure as the body of the actor's act() method.
     */
    public final Actor actor(Closure handler) {
        defaultActor(handler)
    }

    /**
     * Creates a new instance of DefaultThreadActor, using the passed-in closure as the body of the actor's act() method.
     * The actor will stop after one iteration through the passed-in closure.
     */
    public final Actor oneShotActor(Closure handler) {
        defaultOneShotActor(handler)
    }

    /**
     * Creates a new instance of DefaultThreadActor, using the passed-in closure as the body of the actor's act() method.
     */
    public final Actor defaultActor(Closure handler = {throw new UnsupportedOperationException()}) {
        final Actor actor = [act: handler] as DefaultThreadActor
        handler.resolveStrategy=Closure.DELEGATE_FIRST
        handler.delegate = actor
        actor.actorGroup = this
        return actor
    }

    /**
     * Creates a new instance of DefaultThreadActor, using the passed-in closure as the body of the actor's act() method.
     * The actor will stop after one iteration through the passed-in closure.
     */
    public final Actor defaultOneShotActor(Closure handler = {throw new UnsupportedOperationException()}) {
        Closure enhancedHandler = enhanceOneShotHandler(handler)
        final DefaultThreadActor actor = [act: enhancedHandler] as DefaultThreadActor
        handler.resolveStrategy=Closure.DELEGATE_FIRST
        handler.delegate = actor
        enhancedHandler.resolveStrategy=Closure.DELEGATE_FIRST
        enhancedHandler.delegate = actor
        actor.actorGroup = this
        return actor
    }

    /**
     * Creates a new instance of SynchronousActor, using the passed-in closure as the body of the actor's act() method.
     */
    public final Actor synchronousActor(Closure handler = {throw new UnsupportedOperationException()}) {
        final Actor actor = [act: handler] as SynchronousThreadActor
        handler.resolveStrategy=Closure.DELEGATE_FIRST
        handler.delegate = actor
        actor.actorGroup = this
        return actor
    }

    /**
     * Creates a new instance of SynchronousActor, using the passed-in closure as the body of the actor's act() method.
     * The actor will stop after one iteration through the passed-in closure.
     */
    public final Actor synchronousOneShotActor(Closure handler = {throw new UnsupportedOperationException()}) {
        Closure enhancedHandler = enhanceOneShotHandler(handler)
        final Actor actor = [act: enhancedHandler] as SynchronousThreadActor
        handler.resolveStrategy=Closure.DELEGATE_FIRST
        handler.delegate = actor
        enhancedHandler.resolveStrategy=Closure.DELEGATE_FIRST
        enhancedHandler.delegate = actor
        actor.actorGroup = this
        return actor
    }

    /**
     * Creates a new instance of BoundedActor, using the passed-in closure as the body of the actor's act() method.
     */
    public final Actor boundedActor(Closure handler = {throw new UnsupportedOperationException()}) {
        return new InlinedBoundActor(this, handler)
    }

    /**
     * Creates a new instance of BoundedActor, using the passed-in closure as the body of the actor's act() method.
     */
    public final Actor boundedActor(int capacity, Closure handler = {throw new UnsupportedOperationException()}) {
        return new InlinedBoundActor(capacity, handler)
    }

    /**
     * Creates a new instance of BoundedActor, using the passed-in closure as the body of the actor's act() method.
     * The actor will stop after one iteration through the passed-in closure.
     */
    public final Actor boundedOneShotActor(Closure handler = {throw new UnsupportedOperationException()}) {
        Closure enhancedHandler = enhanceOneShotHandler(handler)
        Actor actor = new InlinedBoundActor(this, enhancedHandler)
        handler.resolveStrategy=Closure.DELEGATE_FIRST
        handler.delegate = actor
        actor.actorGroup = this
        return actor
    }

    /**
     * Creates a new instance of BoundedActor, using the passed-in closure as the body of the actor's act() method.
     * The actor will stop after one iteration through the passed-in closure.
     */
    public final Actor boundedOneShotActor(int capacity, Closure handler = {throw new UnsupportedOperationException()}) {
        Closure enhancedHandler = enhanceOneShotHandler(handler)
        Actor actor = new InlinedBoundActor(capacity, enhancedHandler)
        handler.delegate = actor
        handler.resolveStrategy=Closure.DELEGATE_FIRST
        actor.actorGroup = this
        return actor
    }

    private static Closure enhanceOneShotHandler(Closure handler) {
        assert handler != null
        return {
            try {
                handler()
            } finally {
                stop()
            }
        }
    }
}

final class InlinedBoundActor extends BoundThreadActor {

    final Closure handler

    def InlinedBoundActor(AbstractThreadActorGroup actorGroup, Closure handler) {
        this.handler = handler
        this.actorGroup = actorGroup
        handler.delegate = this
        handler.resolveStrategy=Closure.DELEGATE_FIRST
    }

    def InlinedBoundActor(final int capacity, Closure handler) {
        super(capacity);
        this.handler = handler
        handler.delegate = this
        handler.resolveStrategy=Closure.DELEGATE_FIRST
    }

    @Override
    protected void act() {
        handler.call()
    }
}
