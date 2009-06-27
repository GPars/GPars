package org.gparallelizer.actors.pooledActors
/**
 * A pooled actor allowing for an alternative structure of the message handling code.
 * In general DynamicDispatchActor repeatedly scans for messages and dispatches arrived messages to one
 * of the onMessage(message) methods defined on the actor.
 * <pre>
 * final class MyActor extends DynamicDispatchActor {
 *
 *     void onMessage(String message) {
 *         println 'Received string'
 *     }
 *
 *     void onMessage(Integer message) {
 *         println 'Received integer'
 *     }
 *
 *     void onMessage(Object message) {
 *         println 'Received object'
 *     }
 * }
 * </pre>
 * The dispatch leverages Groovy dynamic method dispatch.
 *
 * @author Vaclav Pech
 * Date: Jun 26, 2009
 */

public abstract class DynamicDispatchActor extends AbstractPooledActor {

    /**
     * Loops reading messages using the react() method and dispatches to the corresponding onMessage() method.
     */
    final void act() {
        loop {
            react {
                onMessage it
            }
        }
    }
}