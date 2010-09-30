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
import org.codehaus.groovy.runtime.MethodClosure;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

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
public abstract class AbstractAltSelect<T> {

    private final List<DataFlowReadChannel<? extends T>> channels;
    private final int numberOfChannels;
    private boolean waitingForValue = false;

    @SuppressWarnings({"UnsecureRandomNumberGeneration"})
    private final Random position = new Random();

    public AbstractAltSelect(final DataFlowReadChannel<? extends T>... channels) {
        this.channels = Collections.unmodifiableList(Arrays.asList(channels));
        numberOfChannels = channels.length;
        for (int i = 0; i < channels.length; i++) {
            final DataFlowReadChannel<? extends T> channel = channels[i];
            final int index = i;
            channel.wheneverBound(new MethodClosure(new Runnable() {
                @Override
                public void run() {
                    boundNotification(index, channel);

                }
            }, "run"));
        }
    }

    public final void boundNotification(final int index, final DataFlowReadChannel<? extends T> channel) {
        synchronized (channels) {
            if (waitingForValue) {
                try {
                    final T value = channel.poll();
                    if (value != null) {
                        propagateValue(index, value);
                        waitingForValue = false;
                    }
                } catch (InterruptedException e) {
                    //todo test poll
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }

    }

    abstract void propagateValue(final int index, final T value);

    public T select() {


    }

    public T select(final List<Boolean> mask) throws InterruptedException {
        final int startPosition = position.nextInt(numberOfChannels);

        synchronized (channels) {
            for (int i = 0; i < numberOfChannels; i++) {
                final int currentPosition = (startPosition + i) % numberOfChannels;
                if (mask.get(currentPosition)) {
                    final T value = channels.get(currentPosition).poll();
                    if (value != null) return value;
                }
            }

            //todo propagate the mask
            waitingForValue = true;
        }
    }

    public T prioritySelect() {

    }

    public T prioritySelect(final List<Boolean> mask) {

    }

    /**
     * Reads the next value to output
     *
     * @return The value received from one of the input channels, which is now to be consumed by the user
     * @throws InterruptedException If the current thread gets interrupted inside the method call
     */
    private T doSelect() throws InterruptedException {
        return outputChannel.getVal();
    }

    /**
     * Reads the next value to output
     *
     * @return The value received from one of the input channels, which is now to be consumed by the user
     * @throws InterruptedException If the current thread gets interrupted inside the method call
     */
    public final Object call() throws InterruptedException {
        checkAlive();
        return doSelect();
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
    public final void getValAsync(final MessageStream callback) {
        checkAlive();
        outputChannel.getValAsync(callback);
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
    public final void getValAsync(final Object attachment, final MessageStream callback) {
        checkAlive();
        outputChannel.getValAsync(attachment, callback);
    }

    /**
     * Reads the next value to output
     *
     * @return The value received from one of the input channels, which is now to be consumed by the user
     * @throws InterruptedException If the current thread gets interrupted inside the method call
     */
    @Override
    public final T getVal() throws InterruptedException {
        checkAlive();
        return doSelect();
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
    public final T getVal(final long timeout, final TimeUnit units) throws InterruptedException {
        checkAlive();
        return outputChannel.getVal(timeout, units);
    }

    /**
     * Schedule closure to be executed by pooled actor after data became available
     * It is important to notice that even if data already available the execution of closure
     * will not happen immediately but will be scheduled
     *
     * @param closure closure to execute when data available
     */
    @Override
    public final void rightShift(final Closure closure) {
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

    @Override
    public void wheneverBound(final Closure closure) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void wheneverBound(final MessageStream stream) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isBound() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private void checkAlive() {
        if (!active) throw new IllegalStateException(THE_SELECT_HAS_BEEN_STOPPED_ALREADY);
    }

    /**
     * Retrieves a dataflow channel through which all values are output
     *
     * @return The dataflow channel delivering all output values
     */
    public final DataFlowReadChannel<?> getOutputChannel() {
        return outputChannel;
    }

    /**
     * Stops the internal machinery of the Select instance
     */
    public final void close() {
        selector.stop();
        active = false;
    }

    @SuppressWarnings({"FinalizeDeclaration", "ProhibitedExceptionDeclared"})
    @Override
    protected final void finalize() throws Throwable {
        close();
        super.finalize();
    }
}
