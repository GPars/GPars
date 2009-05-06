package org.gparallelizer.enhancer

import org.gparallelizer.actors.DefaultActor
import org.gparallelizer.enhancer.AsyncMessage
import org.gparallelizer.enhancer.EnhancerHelper

/**
 * The thread-bound actor used in ActorMetaClass
 *
 * @author Vaclav Pech
 * Date: Apr 28, 2009
 */
public class EnhancerActor extends DefaultActor {

    def EnhancerActor() {
        super();
    }

    protected void act() {
        receive {AsyncMessage message -> EnhancerHelper.instance.processMessage(message)}
    }
}