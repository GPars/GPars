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

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author Vaclav Pech
 *         Date: 30th Sep 2010
 */
public class AltSelect<T> {

    private final SelectBase<T> selectBase;

    public AltSelect(final DataFlowReadChannel<T>... channels) {
        selectBase = new SelectBase<T>(channels);
    }

    public SelectResult<T> select() throws InterruptedException {
        return select(-1, null);
    }

    public SelectResult<T> select(final List<Boolean> mask) throws InterruptedException {
        return select(-1, mask);
    }

    //todo timeout select

    public SelectResult<T> prioritySelect() throws InterruptedException {
        return select(0, null);

    }

    public SelectResult<T> prioritySelect(final List<Boolean> mask) throws InterruptedException {
        return select(0, mask);
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
     * @param mask
     * @return The value received from one of the input channels, which is now to be consumed by the user
     * @throws InterruptedException If the current thread gets interrupted inside the method call
     */
    public final SelectResult<T> call(final List<Boolean> mask) throws InterruptedException {
        return select(mask);
    }

    private SelectResult<T> select(final int startIndex, final List<Boolean> mask) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final int[] foundIndex = new int[1];
        @SuppressWarnings({"unchecked"}) final T[] foundValue = (T[]) new Object[1];

        selectBase.doSelect(startIndex, new MaskSelectRequest<T>(mask) {
            @Override
            public void valueFound(final int index, final T value) {
                System.out.println("4 " + index + ":" + value);
                foundIndex[0] = index;
                foundValue[0] = value;
                latch.countDown();
            }
        });
        latch.await();
        return new SelectResult<T>(foundIndex[0], foundValue[0]);
    }
}
