package org.gparallelizer.enhancer

import org.gparallelizer.actors.pooledActors.AbstractPooledActor

/**
 * The event-driven actor used in ActorMetaClass
 *
 * @author Vaclav Pech
 * Date: Apr 28, 2009
 */
public class EnhancerPooledActor extends AbstractPooledActor {

    protected void act() {
        loop {
            react {AsyncMessage message -> EnhancerHelper.instance.processMessage(message) }
        }
    }

}