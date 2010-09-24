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


import groovyx.gpars.group.PGroup

/**
 * Allows repeatedly receive a value across multiple dataflow channels.
 * Whenever a value is available in any of the channels, the value becomes available on the Select itself
 * through its val property.
 * Alternatively timed getVal method can be used, as well as getValAsync() for asynchronous value retrieval
 * or the call() method for nicer syntax.
 * <p/>
 * The output values can also be consumed through the channel obtained from the getOutputChannel method.
 * <p/>
 * This implementation will preserve order of values coming through the same channel, while doesn't give any guaranties
 * about order of messages coming through different channels.
 *
 * @author Vaclav Pech
 *         Date: 21st Sep 2010
 */
public final class Select extends AbstractSelect {

    /**
     * Creates a new Select instance scanning the input channels using threads from the given parallel group's thread pool
     *
     * @param parallelGroup The group to attach to the internal actor
     * @param channels The channels to monitor for values
     */
    Select(final Closure itemFactory = {item, index -> item}, final PGroup parallelGroup, final DataFlowChannel... channels) {
        outputChannel = new DataFlowStream<Object>();
        selector = parallelGroup.selector([inputs: Arrays.asList(channels), outputs: [outputChannel]],
                {item, index ->
                    bindOutput itemFactory(item, index)
                })
    }
}
