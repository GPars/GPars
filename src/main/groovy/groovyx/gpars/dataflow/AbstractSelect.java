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

import groovyx.gpars.dataflow.operator.DataFlowProcessor;

/**
 * Allows repeatedly receive a value across multiple dataflow channels.
 * Whenever a value is available in any of the channels, the value becomes available on the Select itself
 * through its val property.
 * Alternatively timed getVal method can be used, as well as getValAsync() for asynchronous value retrieval
 * or the call() method for nicer syntax.
 * <p/>
 * The output values can also be consumed through the channel obtained from the getOutputChannel method.
 * <p/>
 * Implementations may vary in how they order incoming values on their output.
 *
 * @author Vaclav Pech
 *         Date: 21st Sep 2010
 */
class AbstractSelect {
    protected DataFlowProcessor selector;
    private volatile boolean active = true;
    protected DataFlowChannel<?> outputChannel = null;
    private static final String THE_SELECT_HAS_BEEN_STOPPED_ALREADY = "The Select has been stopped already.";

    protected AbstractSelect() {
    }

    /**
     * Reads the next value to output
     *
     * @return The value received from one of the input channels, which is now to be consumed by the user
     * @throws InterruptedException If the current thread gets interrupted inside the method call
     */
    private Object doSelect() throws InterruptedException {
        return outputChannel.getVal();
    }

    /**
     * Reads the next value to output
     *
     * @return The value received from one of the input channels, which is now to be consumed by the user
     * @throws InterruptedException If the current thread gets interrupted inside the method call
     */
    public final Object call() throws InterruptedException {
        if (!active) throw new IllegalStateException(THE_SELECT_HAS_BEEN_STOPPED_ALREADY);
        return doSelect();
    }

    /**
     * Reads the next value to output
     *
     * @return The value received from one of the input channels, which is now to be consumed by the user
     * @throws InterruptedException If the current thread gets interrupted inside the method call
     */
    public final Object getVal() throws InterruptedException {
        if (!active) throw new IllegalStateException(THE_SELECT_HAS_BEEN_STOPPED_ALREADY);
        return doSelect();
    }

    /**
     * Retrieves a dataflow channel through which all values are output
     *
     * @return The dataflow channel delivering all output values
     */
    public final DataFlowChannel<?> getOutputChannel() {
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
