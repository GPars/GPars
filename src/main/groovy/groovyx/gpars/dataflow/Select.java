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
import groovyx.gpars.group.PGroup;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * A Select allows the user to select a value from multiple channels, which have a value available for read at the moment.
 * It can either pick a channel randomly, when using the plain select method, or with precedence towards channels with lower position indexes,
 * when the prioritySelect method is used.
 * If a value is not available immediately in any of the channels, Select will wait for the first value to arrive in any of the channels.
 * <p/>
 * Both 'select' and 'prioritySelect' methods come in two flavours - blocking, which wait till a value is available in a channel,
 * and messaging, which send out a message to the specified message handler, as soon as a message is available.
 * Optionally, all methods allow the user to specify a boolean mask, assigning each select 's input channel a flag indicating,
 * whether it should be included in the select operation. This is useful when handling state to selectively block some inputs
 * in some states.
 *
 * @author Vaclav Pech
 *         Date: 30th Sep 2010
 */
public class Select<T> {

    private final SelectBase<T> selectBase;

    /**
     * @param pGroup   The group, the thread pool of which should be used for notification message handlers
     * @param channels
     */
    @SuppressWarnings({"OverloadedVarargsMethod"})
    public Select(final PGroup pGroup, final DataFlowReadChannel<? extends T>... channels) {
        selectBase = new SelectBase<T>(pGroup, Arrays.asList(channels));
    }

    /**
     * @param pGroup   The group, the thread pool of which should be used for notification message handlers
     * @param channels
     */
    public Select(final PGroup pGroup, final List<DataFlowReadChannel<? extends T>> channels) {
        //noinspection unchecked
        selectBase = new SelectBase<T>(pGroup, channels);
    }

    public SelectResult<T> select() throws InterruptedException {
        return select(-1, null);
    }

    public SelectResult<T> select(final List<Boolean> mask) throws InterruptedException {
        return select(-1, mask);
    }

    public void select(final MessageStream messageStream) throws InterruptedException {
        select(messageStream, -1, null);
    }

    public void select(final MessageStream messageStream, final List<Boolean> mask) throws InterruptedException {
        select(messageStream, -1, mask);
    }

    public SelectResult<T> prioritySelect() throws InterruptedException {
        return select(0, null);
    }

    public SelectResult<T> prioritySelect(final List<Boolean> mask) throws InterruptedException {
        return select(0, mask);
    }

    public void prioritySelect(final MessageStream messageStream) throws InterruptedException {
        select(messageStream, 0, null);
    }

    public void prioritySelect(final MessageStream messageStream, final List<Boolean> mask) throws InterruptedException {
        select(messageStream, 0, mask);
    }

    /**
     * Reads the next value to output
     *
     * @return The value received from one of the input channels, which is now to be consumed by the user
     * @throws InterruptedException If the current thread gets interrupted inside the method call
     */
    public final SelectResult<T> call() throws InterruptedException {
        return select();
    }

    /**
     * Reads the next value to output
     *
     * @param mask A list of boolean values indicating, which channel should be attempted to read and which not
     * @return The value received from one of the input channels, which is now to be consumed by the user
     * @throws InterruptedException If the current thread gets interrupted inside the method call
     */
    public final SelectResult<T> call(final List<Boolean> mask) throws InterruptedException {
        return select(mask);
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

    private SelectResult<T> select(final int startIndex, final List<Boolean> mask) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final int[] foundIndex = new int[1];
        @SuppressWarnings({"unchecked"}) final T[] foundValue = (T[]) new Object[1];

        selectBase.doSelect(startIndex, new GuardedSelectRequest<T>(mask) {
            @Override
            public void valueFound(final int index, final T value) {
                foundIndex[0] = index;
                foundValue[0] = value;
                latch.countDown();
            }
        });
        latch.await();
        return new SelectResult<T>(foundIndex[0], foundValue[0]);
    }
}
