package org.gparallelizer.actors.pooledActors

import org.gparallelizer.actors.pooledActors.AbstractPooledActor

/**
 * An actor representing a reactor. When it receives a message, the supplied block of code is run with the message
 * as a parameter and the result of the code is send in reply.
 *
 * <pre>
 * final def doubler = reactor {message ->
 *     2 * message
 * }
 *
 * def result = doubler.sendAndWait(10)
 *
 * </pre>
 *
 * @author Vaclav Pech
 * Date: Jun 26, 2009
 */
public class ReactiveActor extends AbstractPooledActor {
    Closure body

    void act() {
        body.delegate = this
        loop {
            react {
                it.replyIfExists body(it)
            }
        }
    }
}