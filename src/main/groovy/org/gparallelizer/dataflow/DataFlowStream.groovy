package org.gparallelizer.dataflow

import java.util.concurrent.LinkedBlockingQueue

/**
 * Represents a thread-safe data flow stream. Values or DataFlowVariables are added using the '<<' operator
 * and safely read once available using the '~' operator.
 * The iterative methods like each(), collect(), iterator(), any(), all() or the for loops work with snapshots
 * of the stream at the time of calling the particular method.
 *
 * @author Vaclav Pech
 * Date: Jun 5, 2009
 */
public final class DataFlowStream<T> {
    /**
     * Stores the DataFlowVariables in the buffer.
     */
    private LinkedBlockingQueue<DataFlowVariable<T>> queue = new LinkedBlockingQueue<DataFlowVariable<T>>()

    /**
     * Adds a DataFlowVariable to the buffer.
     */
    public void leftShift(DataFlowVariable<T> ref) {
        queue.offer(ref)
    }

    /**
     * Adds a DataFlowVariable representing the passed in value to the buffer.
     */
    public void leftShift(T value) {
        final def ref = new DataFlowVariable<T>()
        ref << value
        queue.offer(ref)
    }

    /**
     * Retrieves the value at the head of the buffer. Blocks until a value is available.
     */
    public T bitwiseNegate() {
        return ~(queue.take())
    }

    /**
     * Retrieves the DataFlowVariable at the head of the buffer. Blocks until the buffer is not empty.
     */
    public DataFlowVariable<T> take() {
        queue.take()
    }

    /**
     * Returns the current size of the buffer
     */
    public int length() {
        queue.size()
    }

    /**
     * Returns an iterator over a current snapshot of the buffer's content. The next() method returns actual values
     * not the DataFlowVariables.
     */
    public Iterator iterator() {
        final def iterator = queue.iterator()
        [
                hasNext: {iterator.hasNext()},
                next: {~(iterator.next())}
        ] as Iterator
    }
}