package org.gparallelizer.actors;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Provides an Actor implementation with bounded fair ArrayBlockingQueue storing the messages.
 * The send() method will wait for space to become available in the queue, if it is full.
 *
 * @author Vaclav Pech
 * Date: Jan 7, 2009
 */
public class BoundedThreadActor extends AbstractThreadActor {

    public static final int DEFAULT_CAPACITY=1000;

    def BoundedThreadActor() {
        this(DEFAULT_CAPACITY);
    }

    def BoundedThreadActor(int capacity) {
        super(new ArrayBlockingQueue<ActorMessage>(capacity, false));
    }
}