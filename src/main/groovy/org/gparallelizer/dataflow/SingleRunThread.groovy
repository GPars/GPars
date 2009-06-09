package org.gparallelizer.dataflow

/**
 * An actor representing a dataflow thread. Runs the supplied block of code inside the act() actor method.
 *
 * @author Vaclav Pech
 * Date: Jun 5, 2009
 */
final class SingleRunThread extends DataFlowActor {

    Closure body

    void act() {
        body.delegate = this
        body()
    }
}

//private class ReactiveEventBasedThread extends DataFlowActor {
//
//    Closure body
//
//    //todo reconsider the loop
//    //todo reconsider the method
//    void act() {
//        body.delegate = this
//        loop {
//            react {
//                it.reply body(it)
//            }
//        }
//    }
//}

