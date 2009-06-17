package org.gparallelizer.actors;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Provides a default Actor implementation with unbounded LinkedBlockingQueue storing the messages.
 *
 * @author Vaclav Pech
 * Date: Jan 7, 2009
 */
public class DefaultThreadActor extends AbstractThreadActor {
    def DefaultThreadActor() {
        super(new LinkedBlockingQueue<ActorMessage>());
    }
}