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

/**
 * @author Alex Tkachman
 */
public abstract class DataFlowComplexExpression<T> extends DataFlowExpression<T> {
    protected Object[] args;

    protected DataFlowComplexExpression(final Object... elements) {
        this.args = elements;
    }

    @Override
    protected void subscribe(final DataFlowExpressionsCollector listener) {
        for (int i = 0; i != args.length; ++i) {
            args[i] = listener.subscribe(args[i]);
        }
    }

    @Override
    protected T evaluate() {
        for (int i = 0; i != args.length; ++i) {
            if (args[i] instanceof DataFlowExpression) {
                args[i] = ((DataFlowExpression) args[i]).value;
            }
        }

        return null;
    }
}
