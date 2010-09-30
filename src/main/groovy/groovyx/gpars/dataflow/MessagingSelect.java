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

import java.util.Arrays;
import java.util.List;

/**
 * @author Vaclav Pech
 *         Date: 30th Sep 2010
 */
public class MessagingSelect<T> {

    private final SelectBase<T> selectBase;

    @SuppressWarnings({"OverloadedVarargsMethod"})
    public MessagingSelect(final DataFlowReadChannel<? extends T>... channels) {
        selectBase = new SelectBase<T>(Arrays.asList(channels));
    }

    public MessagingSelect(final List<DataFlowReadChannel<? extends T>> channels) {
        //noinspection unchecked
        selectBase = new SelectBase<T>(channels);
    }

    public void select(final MessageStream messageStream) throws InterruptedException {
        select(messageStream, -1, null);
    }

    public void select(final MessageStream messageStream, final List<Boolean> mask) throws InterruptedException {
        select(messageStream, -1, mask);
    }

    public void prioritySelect(final MessageStream messageStream) throws InterruptedException {
        select(messageStream, 0, null);
    }

    public void prioritySelect(final MessageStream messageStream, final List<Boolean> mask) throws InterruptedException {
        select(messageStream, 0, mask);
    }

    public final void call(final MessageStream messageStream) throws InterruptedException {
        select(messageStream);
    }

    public final void call(final MessageStream messageStream, final List<Boolean> mask) throws InterruptedException {
        select(messageStream, mask);
    }

    private void select(final MessageStream messageStream, final int startIndex, final List<Boolean> mask) throws InterruptedException {
        selectBase.doSelect(startIndex, new GuardedSelectRequest<T>(mask) {
            @Override
            public void valueFound(final int index, final T value) {
                messageStream.send(new SelectResult<T>(index, value));
            }
        });
    }
}
