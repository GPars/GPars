//  GParallelizer
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License. 

package org.gparallelizer.dataflow;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Represents a thread-safe data flow stream. Values or DataFlowVariables are added using the '<<' operator
 * and safely read once available using the '()' operator.
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
    private final LinkedBlockingQueue<DataFlowVariable<T>> queue = new LinkedBlockingQueue<DataFlowVariable<T>>();

    /**
     * Adds a DataFlowVariable to the buffer.
     */
    public void leftShift(final DataFlowVariable<T> ref) {
        queue.offer(ref);
    }

    /**
     * Adds a DataFlowVariable representing the passed in value to the buffer.
     */
    public void leftShift(final T value) {
        final DataFlowVariable<T> ref = new DataFlowVariable<T>();
        ref.bind(value);
        queue.offer(ref);
    }

    /**
     * Retrieves the value at the head of the buffer. Blocks until a value is available.
     */
    public T getVal() throws InterruptedException {
        return queue.take().getVal();
    }

    /**
     * Retrieves the DataFlowVariable at the head of the buffer. Blocks until the buffer is not empty.
     */
    public DataFlowVariable<T> take() throws InterruptedException {
        return queue.take();
    }

    /**
     * Returns the current size of the buffer
     */
    public int length() {
        return queue.size();
    }

    /**
     * Returns an iterator over a current snapshot of the buffer's content. The next() method returns actual values
     * not the DataFlowVariables.
     */
    public Iterator iterator() {
        final Iterator<DataFlowVariable<T>> iterator = queue.iterator();
        return new Iterator<T>() {

            public boolean hasNext() {
                return iterator.hasNext();
            }

            public T next() {
                try {
                    return iterator.next().getVal();
                } catch (InterruptedException e) {
                    throw new IllegalStateException("The thread has been interrupted, which prevented the iterator from retrieving the next element.");
                }
            }

            public void remove() {
                throw new UnsupportedOperationException("Remove not available");
            }
        };

    }

    @Override public String toString() {
        return "DataFlowStream(queue=" + new ArrayList<DataFlowVariable<T>>(queue).toString() + ")" ;
    }
}
