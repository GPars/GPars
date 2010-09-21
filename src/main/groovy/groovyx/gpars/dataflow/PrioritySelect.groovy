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

package groovyx.gpars.dataflow

import groovyx.gpars.actor.impl.MessageStream
import groovyx.gpars.group.PGroup
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.TimeUnit

/**
 *
 * @author Vaclav Pech
 * Date: 21st Sep 2010
 */
public final class PrioritySelect extends AbstractSelect {
    long counter = 0
    final PriorityBlockingQueue queue = new PriorityBlockingQueue(11, {a, b -> a.index <=> b.index ?: a.counter <=> b.counter} as Comparator)
    final PrioritySelectChannel outputChannel

    def PrioritySelect(final PGroup parallelGroup, final DataFlowChannel... channels) {
        outputChannel = new PrioritySelectChannel(queue)
        selector = parallelGroup.selector([inputs: Arrays.asList(channels), outputs: []],
                {item, index ->
                    queue.add([item: item, index: index, counter: counter++])
                    outputChannel.valueArrived()
                })
    }

    @Override
    def doSelect() {
        outputChannel.val
    }

    @Override
    public DataFlowChannel getOutputChannel() {
        outputChannel
    }
}

//todo java
//todo output channel enhancements and tests
//Different threads may call the callbacks
//synchronous and asynchronous value retrievals may not be ordered
private class PrioritySelectChannel implements DataFlowChannel {
    private PriorityBlockingQueue queue
    private final List queuedCallbacks = []

    def PrioritySelectChannel(final queue) {
        this.queue = queue;
    }

    void valueArrived() {
        def callback
        def value
        synchronized (queuedCallbacks) {
            callback = queuedCallbacks[0]
            if (callback == null) return
            value = queue.poll()
            if (value == null) return
            queuedCallbacks.remove(0)          //We hold the callback already
        }
        if (callback[0] == null) {  //no attachment
            callback[1].send(value.item)
        } else {
            callback[1].send([attachment: callback[0], result: value.item])
        }
    }

    @Override
    public void getValAsync(final MessageStream callback) {
        getValAsync null, callback
    }

    @Override
    public void getValAsync(Object attachment, MessageStream callback) {
        synchronized (queuedCallbacks) {
            queuedCallbacks.add([attachment, callback])
            valueArrived()
        }
    }

    @Override
    Object getVal() {
        return queue.take().item
    }

    Object getVal(long timeout, TimeUnit units) {
        return queue.poll(timeout, units)?.item
    }
}
