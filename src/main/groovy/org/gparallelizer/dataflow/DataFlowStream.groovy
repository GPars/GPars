package org.gparallelizer.dataflow

import java.util.concurrent.LinkedBlockingQueue

/**
 * Created by IntelliJ IDEA.
 * User: vaclav
 * Date: Jun 5, 2009
 * Time: 8:49:12 AM
 * To change this template use File | Settings | File Templates.
 */

public class DataFlowStream<T> {
    private LinkedBlockingQueue<DataFlowVariable<T>> queue = new LinkedBlockingQueue<DataFlowVariable<T>>()

    public void leftShift(DataFlowVariable<T> ref) {
        queue.offer(ref)
    }

    public void leftShift(T value) {
        final def ref = new DataFlowVariable<T>()
        ref << value
        queue.offer(ref)
    }

    public T bitwiseNegate() {
        return ~(queue.take())
    }

    public DataFlowVariable<T> take() {
        queue.take()
    }

    public int length() {
        queue.size()
    }

    public Iterator iterator() {
        final def iterator = queue.iterator()
        [
                hasNext: {iterator.hasNext()},
                next: {~(iterator.next())}
        ] as Iterator
    }

    //todo toArray
    //todo elementAt
    //todo generics
}