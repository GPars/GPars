package org.gparallelizer.samples.actors

import org.gparallelizer.actors.pooledActors.DynamicDispatchActor

/**
 * Demonstrates use of the DynamicDispatchActor class, which leverages Groovy dynamic method dispatch to invoke
 * the appropriate onMessage() method.
 */

final class MyActor extends DynamicDispatchActor {
    void onMessage(String message) {
        println 'Received string'
    }

    void onMessage(Integer message) {
        println 'Received integer'
    }

    void onMessage(Object message) {
        println 'Received object'
    }

    void onMessage(List message) {
        println 'Received list'
        stop()
    }
}

final def actor = new MyActor().start()

actor  << 1
actor  << ''
actor  << 1.0
actor  << new ArrayList()

actor.join()
