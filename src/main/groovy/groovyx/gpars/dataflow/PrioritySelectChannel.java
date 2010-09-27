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

import groovy.lang.Closure;
import groovyx.gpars.actor.impl.MessageStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;

/**
 * Implements a dataflow channel for values output from a PrioritySelect class.
 * <p/>
 * Limitations:
 * Different threads may call end-up calling the callbacks provided into the getValAsync() method. It can either be the selector's actor thread
 * or the caller calling getValAsync itself.
 * Subsequent synchronous and asynchronous value retrievals may not receive values in the order of their respective calls.
 *
 * @author Vaclav Pech
 *         Date: 21st Sep 2010
 */
class PrioritySelectChannel implements DataFlowChannel<Object> {
    private final PriorityBlockingQueue<Map<String, Object>> queue;
    private final List<List<Object>> queuedCallbacks = new ArrayList<List<Object>>(10);

    PrioritySelectChannel(final PriorityBlockingQueue<Map<String, Object>> queue) {
        //noinspection AssignmentToCollectionOrArrayFieldFromParameter
        this.queue = queue;
    }

    /**
     * Attempts to deliver a value to a queued callback
     */
    void valueArrived() {
        final List<Object> callback;
        final Map<String, Object> value;
        synchronized (queuedCallbacks) {
            if (queuedCallbacks.isEmpty()) return;
            value = queue.poll();
            if (value == null) return;
            callback = queuedCallbacks.remove(0);
        }
        if (callback.get(0) == null) {  //no attachment
            ((MessageStream) callback.get(1)).send(value.get("item"));
        } else {
            final Map<String, Object> message = new HashMap<String, Object>(2);
            message.put(ATTACHMENT, callback.get(0));
            message.put(RESULT, value.get("item"));
            ((MessageStream) callback.get(1)).send(message);
        }
    }

    /**
     * Asynchronously retrieves the value from the channel. Sends the actual value of the channel as a message
     * back the the supplied actor once the value has been bound.
     * The actor can perform other activities or release a thread back to the pool by calling react() waiting for the message
     * with the value of the Dataflow channel.
     *
     * @param callback An actor to send the bound value to.
     */
    @Override
    public void getValAsync(final MessageStream callback) {
        getValAsync(null, callback);
    }

    /**
     * Asynchronously retrieves the value from the channel. Sends a message back the the supplied MessageStream
     * with a map holding the supplied attachment under the 'attachment' key and the actual value of the channel under
     * the 'result' key once the value has been bound.
     * Attachment is an arbitrary value helping the actor.operator match its request with the reply.
     * The actor/operator can perform other activities or release a thread back to the pool by calling react() waiting for the message
     * with the value of the Dataflow channel.
     *
     * @param attachment arbitrary non-null attachment if reader needs better identification of result
     * @param callback   An actor to send the bound value plus the supplied index to.
     */
    @Override
    public void getValAsync(final Object attachment, final MessageStream callback) {
        synchronized (queuedCallbacks) {
            queuedCallbacks.add(asList(attachment, callback));
            valueArrived();
        }
    }

    /**
     * Reads the current value of the channel. Blocks, if the value has not been assigned yet.
     *
     * @return The actual value
     * @throws InterruptedException If the current thread gets interrupted while waiting for the channel to be bound
     */
    @Override
    public Object getVal() throws InterruptedException {
        return queue.take().get("item");
    }

    /**
     * Reads the current value of the channel. Blocks up to given timeout, if the value has not been assigned yet.
     *
     * @param timeout The timeout value
     * @param units   Units for the timeout
     * @return The actual value
     * @throws InterruptedException If the current thread gets interrupted while waiting for the channel to be bound
     */
    @Override
    public Object getVal(final long timeout, final TimeUnit units) throws InterruptedException {
        final Map<String, Object> value = queue.poll(timeout, units);
        if (value != null) return value.get("item");
        else return null;
    }

    /**
     * Schedule closure to be executed by pooled actor after data became available
     * It is important to notice that even if data already available the execution of closure
     * will not happen immediately but will be scheduled
     *
     * @param closure closure to execute when data available
     */
    @Override
    public void rightShift(final Closure closure) {
        whenBound(closure);
    }

    /**
     * Schedule closure to be executed by pooled actor after data becomes available
     * It is important to notice that even if data already available the execution of closure
     * will not happen immediately but will be scheduled.
     *
     * @param closure closure to execute when data available
     */
    @Override
    public final void whenBound(final Closure closure) {
        getValAsync(new DataCallback(closure, DataFlowExpression.retrieveCurrentDFPGroup()));
    }

    /**
     * Send the bound data to provided stream when it becomes available
     *
     * @param stream stream where to send result
     */
    @Override
    public final void whenBound(final MessageStream stream) {
        getValAsync(stream);
    }
}
