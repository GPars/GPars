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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

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
 *         Date: 29th Sep 2010
 */
@SuppressWarnings({"RawUseOfParameterizedType"})
public final class SelectBase<T> {

    private final List<DataFlowReadChannel<? extends T>> channels;
    private final int numberOfChannels;
    private final Collection<SelectRequest<T>> pendingRequests = new ArrayList<SelectRequest<T>>();

    @SuppressWarnings({"UnsecureRandomNumberGeneration"})
    private final Random position = new Random();

    SelectBase(final DataFlowReadChannel<? extends T>... channels) {
        this.channels = Collections.unmodifiableList(Arrays.asList(channels));
        numberOfChannels = channels.length;
        for (int i = 0; i < channels.length; i++) {
            final DataFlowReadChannel<? extends T> channel = channels[i];
            //noinspection ThisEscapedInObjectConstruction
            channel.wheneverBound(new SelectCallback<T>(this, i, channel));
        }
    }

    @SuppressWarnings({"MethodOnlyUsedFromInnerClass"})
    void boundNotification(final int index, final DataFlowReadChannel<? extends T> channel) throws InterruptedException {
        synchronized (channels) {
            for (final SelectRequest<T> selectRequest : pendingRequests) {
                if (selectRequest.matchesMask(index)) {
                    final T value = channel.poll();
                    if (value != null) {
                        pendingRequests.remove(selectRequest);
                        selectRequest.valueFound(index, value);
                        System.out.println("AAAAAAAAAAAAAAAAAAAAa");
                        return;
                    }
                }
            }
        }
    }

    void doSelect(final int startIndex, final SelectRequest<T> selectRequest) throws InterruptedException {
        final int startPosition = startIndex == -1 ? position.nextInt(numberOfChannels) : startIndex;

        synchronized (channels) {
            for (int i = 0; i < numberOfChannels; i++) {
                final int currentPosition = (startPosition + i) % numberOfChannels;
                System.out.println("1");
                if (selectRequest.matchesMask(currentPosition) && !(channels.get(currentPosition) instanceof DataFlowVariable)) {
                    System.out.println("2");
                    final T value = channels.get(currentPosition).poll();
                    if (value != null) {
                        System.out.println("3 " + value);
                        selectRequest.valueFound(currentPosition, value);
                        return;
                    }
                }
            }
            pendingRequests.add(selectRequest);
        }
    }
}
