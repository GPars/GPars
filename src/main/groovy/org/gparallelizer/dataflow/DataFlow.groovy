package org.gparallelizer.dataflow

import org.gparallelizer.actors.pooledActors.PooledActorGroup
import org.gparallelizer.actors.pooledActors.PooledActor

/**
 * Created by IntelliJ IDEA.
 * User: vaclav
 * Date: Jun 4, 2009
 * Time: 1:21:43 PM
 * To change this template use File | Settings | File Templates.
 */

public class DataFlow {

    public static PooledActor thread(final Closure code) {
        new SingleRunThread(body: code).start()
    }

    //todo reconsider need for it
//    public static <T, V> ReactiveEventBasedThread thread(final Closure<T, V> code) {
//        new ReactiveEventBasedThread(body: code).start()
//    }

}