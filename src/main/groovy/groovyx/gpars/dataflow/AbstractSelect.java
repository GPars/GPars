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
 * @author Vaclav Pech
 *         Date: 21st Sep 2010
 */
abstract class AbstractSelect {
    protected DataFlowProcessor selector;
    private volatile boolean active = true;
    private static final String THE_SELECT_HAS_BEEN_STOPPED_ALREADY = "The Select has been stopped already.";

    protected AbstractSelect() {
    }

    abstract Object doSelect() throws InterruptedException;

    public abstract DataFlowChannel<?> getOutputChannel();

    public final Object call() throws InterruptedException {
        if (!active) throw new IllegalStateException(THE_SELECT_HAS_BEEN_STOPPED_ALREADY);
        return doSelect();
    }

    public final Object getVal() throws InterruptedException {
        if (!active) throw new IllegalStateException(THE_SELECT_HAS_BEEN_STOPPED_ALREADY);
        return doSelect();
    }

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
