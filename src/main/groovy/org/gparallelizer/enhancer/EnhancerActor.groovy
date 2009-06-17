package org.gparallelizer.enhancer

import org.gparallelizer.actors.DefaultThreadActor
import org.gparallelizer.enhancer.AsyncMessage
import org.gparallelizer.enhancer.EnhancerHelper
import org.gparallelizer.actors.DefaultThreadActor

/**
 * The thread-bound actor used in ActorMetaClass
 *
 * @author Vaclav Pech
 * Date: Apr 28, 2009
 */
public class EnhancerActor extends DefaultThreadActor {

    def EnhancerActor() {
        super();
    }

    protected void act() {
        receive {AsyncMessage message -> EnhancerHelper.instance.processMessage(message)}
    }
}