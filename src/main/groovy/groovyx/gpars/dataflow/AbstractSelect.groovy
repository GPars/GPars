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

import groovyx.gpars.dataflow.operator.DataFlowProcessor

/**
 *
 * @author Vaclav Pech
 * Date: 21st Sep 2010
 */
abstract protected class AbstractSelect {
    protected DataFlowProcessor selector
    private volatile boolean active = true

    protected def Select() { }

    abstract def select()

    ;

    public abstract DataFlowChannel getOutputChannel()

    ;

    final public def call() {
        if (!active) throw new IllegalStateException("The Select has been stopped already.")
        select()
    }

    final public def getVal() {
        if (!active) throw new IllegalStateException("The Select has been stopped already.")
        select()
    }

    final public void close() {
        selector.stop()
        active = false
    }

    final protected void finalize() {
        close()
    }
}
