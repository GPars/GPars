package org.gparallelizer.actors;

import java.util.concurrent.SynchronousQueue;


/**
 * Provides an Actor implementation with fair SynchronousQueue storing the messages.
 * The send() method blocks until a call to receive() takes the message for processing.
 *
 * @author Vaclav Pech
 * Date: Jan 7, 2009
 */

public class SynchronousActor extends AbstractActor {
    SynchronousActor() {
        super(new SynchronousQueue(true));
    }
}