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

package groovyx.gpars.dataflow;

import groovyx.gpars.MessagingRunnable;
import groovyx.gpars.actor.impl.MessageStream;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A synchronous variant of DataflowVariable, which blocks the writer as well as the readers.
 * The synchronous variable ensures a specified number of readers must ask for a value before the writer as well as the readers can continue.
 *
 * @author Vaclav Pech
 */
public final class SyncDataflowVariable<T> extends DataflowVariable<T> {
    private static final String ERROR_WRITING_INTO_A_SYNCHRONOUS_CHANNEL = "Error writing into a synchronous channel.";
    private final CountDownLatch parties;

    /**
     * Creates a new variable, which will never block writers.
     */
    public SyncDataflowVariable() {
        this(0);
    }

    /**
     * Creates a new variable blocking the specified number of readers.
     *
     * @param parties Number of readers that have to match a writer before the message gets transferred
     */
    public SyncDataflowVariable(final int parties) {
        this.parties = new CountDownLatch(parties);
    }

    @Override
    protected void doBindImpl(final T value) {
        super.doBindImpl(value);
        awaitParties();
    }

    @Override
    public T getVal() throws InterruptedException {
        final T val = super.getVal();
        readerIsReady();
        return val;
    }

    @Override
    public T getVal(final long timeout, final TimeUnit units) throws InterruptedException {
        final T result = super.getVal(timeout, units);
        if (result == null) {
            if (!this.isBound()) return null;
            final T val = getVal();
            readerIsReady();
            return val;
        }
        readerIsReady();
        return result;
    }

    @Override
    protected void scheduleCallback(final Object attachment, final MessageStream callback) {
        super.scheduleCallback(attachment, new DataCallback(new MessagingRunnable() {
            @Override
            protected void doRun(final Object argument) {
                readerIsReady();
                callback.send(argument);
            }
        }, Dataflow.retrieveCurrentDFPGroup()));
    }

    private void readerIsReady() {
        parties.countDown();
        awaitParties();
    }

    private void awaitParties() {
        try {
            parties.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(ERROR_WRITING_INTO_A_SYNCHRONOUS_CHANNEL, e);
        }
    }
}
