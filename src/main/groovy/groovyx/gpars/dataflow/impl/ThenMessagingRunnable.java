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

package groovyx.gpars.dataflow.impl;

import groovy.lang.Closure;
import groovyx.gpars.MessagingRunnable;
import groovyx.gpars.dataflow.DataflowVariable;

/**
 * Created by IntelliJ IDEA.
 * User: Vaclav
 * Date: 24.11.11
 * Time: 13:46
 * To change this template use File | Settings | File Templates.
 */
public class ThenMessagingRunnable<T, V> extends MessagingRunnable<T> {
    private final DataflowVariable<V> result;
    private final Closure closure;

    public ThenMessagingRunnable(final DataflowVariable<V> result, final Closure closure) {
        this.result = result;
        this.closure = closure;
    }

    @Override
    protected void doRun(final T argument) {
        result.bind((V) closure.call(argument));
    }
}
