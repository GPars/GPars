package org.gparallelizer.dataflow
/**
 * Created by IntelliJ IDEA.
 * User: vaclav
 * Date: Jun 5, 2009
 * Time: 2:27:26 PM
 * To change this template use File | Settings | File Templates.
 */

class SingleRunThread extends DataFlowActor {

    Closure body

    void act() {
        body.delegate = this
        body()
    }
}
