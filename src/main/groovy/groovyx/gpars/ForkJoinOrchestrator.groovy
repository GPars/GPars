package groovyx.gpars

import groovyx.gpars.dataflow.DataFlowVariable
import jsr166y.forkjoin.RecursiveAction
import jsr166y.forkjoin.TaskBarrier

/**
 * Created by IntelliJ IDEA.
 * User: vaclav
 * Date: Nov 1, 2009
 * Time: 3:05:14 PM
 * To change this template use File | Settings | File Templates.
 */
public final class ForkJoinOrchestrator<T> extends RecursiveAction {
    DataFlowVariable<T> result = new DataFlowVariable<T>()
    ForkJoinWorker<T> rootWorker

    protected void compute() {
        final TaskBarrier taskBarrier = new TaskBarrier(1)
        rootWorker.taskBarrier = taskBarrier
        rootWorker.fork()
        taskBarrier.arriveAndAwait()
        result << rootWorker.result
    }

    public T getResult() {
        result.val
    }
}
