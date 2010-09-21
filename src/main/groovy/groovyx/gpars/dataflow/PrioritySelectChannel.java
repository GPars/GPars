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

import groovyx.gpars.actor.impl.MessageStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;

/**
 * Different threads may call the callbacks
 * synchronous and asynchronous value retrievals may not be ordered
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

    @Override
    public void getValAsync(final MessageStream callback) {
        getValAsync(null, callback);
    }

    @Override
    public void getValAsync(final Object attachment, final MessageStream callback) {
        synchronized (queuedCallbacks) {
            queuedCallbacks.add(asList(attachment, callback));
            valueArrived();
        }
    }

    @Override
    public Object getVal() throws InterruptedException {
        return queue.take().get("item");
    }

    @Override
    public Object getVal(final long timeout, final TimeUnit units) throws InterruptedException {
        final Map<String, Object> value = queue.poll(timeout, units);
        if (value != null) return value.get("item");
        else return null;
    }
}
