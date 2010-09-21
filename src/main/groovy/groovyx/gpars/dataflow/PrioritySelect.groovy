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

import groovyx.gpars.group.PGroup
import java.util.concurrent.PriorityBlockingQueue

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
