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

package groovyx.gpars.dataflow;

import groovyx.gpars.dataflow.stream.DataFlowStream;
import groovyx.gpars.dataflow.stream.DataFlowStreamReadAdapter;
import groovyx.gpars.dataflow.stream.DataFlowStreamWriteAdapter;

/**
 * Offers a deterministic one-to-many and many-to-many messaging alternative to DataFlowQueue.
 * Internally it wraps a DataFlowStream class with a DataFlowStreamWriteAdapter and so
 * synchronizes all writes to the underlying stream allowing multiple threads accessing the stream concurrently.
 * On demand through the createReadChannel() method it will return an DataFlowReadChannel through which the reader will receive
 * all messages written to the channel since then.
 * <p/>
 * Typical use:
 * <p/>
 * DataFlowWriteChannel broadcastStream = new DataFlowBroadcast()
 * DataFlowReadChannel stream1 = broadcastStream.createReadChannel()
 * DataFlowReadChannel stream2 = broadcastStream.createReadChannel()
 * broadcastStream << 'Message'
 * assert stream1.val == stream2.val
 *
 * @param <T> The type of messages to pass through the stream
 * @author Vaclav Pech
 */
public final class DataFlowBroadcast<T> extends DataFlowStreamWriteAdapter<T> {

    /**
     * Creates a new adapter
     */
    public DataFlowBroadcast() {
        super(new DataFlowStream<T>());
    }

    @SuppressWarnings({"SynchronizedMethod"})
    @Override
    public synchronized String toString() {
        return "DataFlowBroadcast around " + super.toString();
    }

    /**
     * Retrieves an implementation of DataFlowReadChannel to read all messages submitted to the broadcast chanel.
     * Since multiple parties (threads/tasks/actors/...) may ask for read channels independently, the submitted messages are effectively
     * broadcast to all the subscribers.
     *
     * @return A read channel to receive messages submitted to the broadcast channel from now on.
     */
    public DataFlowReadChannel<T> createReadChannel() {
        return new DataFlowStreamReadAdapter<T>(getHead());
    }
}

