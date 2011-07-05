// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
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

import groovyx.gpars.dataflow.DataflowReadChannel;
import groovyx.gpars.dataflow.DataflowWriteChannel;

import java.util.concurrent.BrokenBarrierException;

/**
 * Adapts a DataflowStream to accommodate for the DataflowWriteChannel interface.
 * It also synchronizes all changes to the underlying stream allowing multiple threads accessing the stream concurrently.
 *
 * @param <T> The type of messages to pass through the stream
 * @author Vaclav Pech
 */
@SuppressWarnings({"SynchronizedMethod"})
public class SynchronousStreamWriteAdapter<T> implements DataflowWriteChannel<T> {

    private static final String ERROR_WRITING_INTO_A_SYNCHRONOUS_CHANNEL = "Error writing into a synchronous channel.";
    private int numberOfSubscriptions = 1;  //one for the writer party
    private final DataflowStreamWriteAdapter<SynchronousValue<T>> internalStream;

    /**
     * Creates a new adapter
     *
     * @param stream The stream to wrap
     */
    public SynchronousStreamWriteAdapter(final StreamCore<SynchronousValue<T>> stream) {
        internalStream = new DataflowStreamWriteAdapter<SynchronousValue<T>>(stream);
    }

    @Override
    public final synchronized DataflowWriteChannel<T> leftShift(final T value) {
        final SynchronousValue<T> synchronousValue = new SynchronousValue<T>(value, numberOfSubscriptions);
        internalStream.leftShift(synchronousValue);
        awaitParties(synchronousValue);
        return this;
    }

    protected synchronized void increaseNumberOFSubscriptions() {
        numberOfSubscriptions++;
    }

    protected synchronized void decreaseNumberOFSubscriptions() {
        if (numberOfSubscriptions == 1)
            throw new IllegalStateException("Cannot decrease the number of subscriptions below 1.");
        numberOfSubscriptions--;
    }

    private void awaitParties(final SynchronousValue<T> synchronousValue) {
        try {
            synchronousValue.getBarrier().await();
        } catch (InterruptedException e) {
            throw new RuntimeException(ERROR_WRITING_INTO_A_SYNCHRONOUS_CHANNEL, e);
        } catch (BrokenBarrierException e) {
            throw new RuntimeException(ERROR_WRITING_INTO_A_SYNCHRONOUS_CHANNEL, e);
        }
    }

    @Override
    public final synchronized DataflowWriteChannel<T> leftShift(final DataflowReadChannel<T> ref) {
        //todo handle
//        final SynchronousValue<T> synchronousValue = new SynchronousValue<T>(ref, numberOfSubscriptions);
//        head.leftShift(synchronousValue);
//        head = (DataflowStream<T>) head.getRest();
//        awaitParties(synchronousValue);
        return this;
    }

    @Override
    public final void bind(final T value) {
        this.leftShift(value);
    }

    @Override
    public String toString() {
        return internalStream.toString();
    }

    protected final StreamCore<SynchronousValue<T>> getHead() {
        return internalStream.getHead();
    }
}

