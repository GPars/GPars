// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.dataflow.stream;

import groovyx.gpars.dataflow.DataFlowReadChannel;
import groovyx.gpars.dataflow.DataFlowWriteChannel;

/**
 * Adapts a DataFlowStream to accommodate for the DataFlowWriteChannel interface.
 * It also synchronizes all changes to the underlying stream allowing multiple threads accessing the stream concurrently.
 *
 * @param <T> The type of messages to pass through the stream
 * @author Vaclav Pech
 */
@SuppressWarnings({"SynchronizedMethod"})
public class DataFlowStreamWriteAdapter<T> implements DataFlowWriteChannel<T> {

    private DataFlowStream<T> head;

    /**
     * Creates a new adapter
     *
     * @param stream The stream to wrap
     */
    public DataFlowStreamWriteAdapter(final DataFlowStream<T> stream) {
        this.head = stream;
    }

    @Override
    public synchronized DataFlowWriteChannel<T> leftShift(final T value) {
        head.leftShift(value);
        head = (DataFlowStream<T>) head.getRest();
        return this;
    }

    @Override
    public synchronized DataFlowWriteChannel<T> leftShift(final DataFlowReadChannel<T> ref) {
        head.leftShift(ref);
        head = (DataFlowStream<T>) head.getRest();
        return this;
    }

    @Override
    public synchronized void bind(final T value) {
        head.leftShift(value);
        head = (DataFlowStream<T>) head.getRest();
    }

    @Override
    public synchronized String toString() {
        return head.toString();
    }
}

