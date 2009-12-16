//  GPars (formerly GParallelizer)
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package groovyx.gpars.dataflow;

import org.codehaus.groovy.runtime.InvokerHelper;

/**
 * DFE which evaluate property when receiver became available
 *
 * @author Alex Tkachman
 */
public class DataFlowGetPropertyExpression<T> extends DataFlowExpression<T> {
    private static final long serialVersionUID = 2984824057556784227L;
    private final DataFlowExpression receiver;
    private final String name;

    public DataFlowGetPropertyExpression(final DataFlowExpression expression, final String name) {
        this.receiver = expression;
        this.name = name;
        subscribe();
    }

    @Override protected void subscribe(final DataFlowExpressionsCollector listener) {
        listener.subscribe(receiver);
    }

    @Override @SuppressWarnings("unchecked")
    protected T evaluate() {
        //noinspection unchecked
        return (T) InvokerHelper.getProperty(receiver.value, name);
    }
}
