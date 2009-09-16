//  GParallelizer
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

package org.gparallelizer.dataflow;

import org.gparallelizer.MessageStream;
import org.gparallelizer.remote.serial.RemoteSerialized;
import org.gparallelizer.remote.RemoteHost;

import java.io.ObjectStreamException;

/**
 * Represents a thread-safe single-assignment, multi-read variable.
 * Each instance of DataFlowVariable can be read repeatedly any time using the 'val' property and assigned once
 * in its lifetime using the '<<' operator. Reads preceding assignment will be blocked until the value
 * is assigned.
 * For actors and Dataflow Operators the asynchronous non-blocking variants of the getValAsync() methods can be used.
 * They register the request to read a value and will send a message to the actor or operator once the value is available.
 *
 * @author Vaclav Pech, Alex Tkachman
 *         Date: Jun 4, 2009
 * @param <T> Type of values to bind with the DataFlowVariable
 */
@SuppressWarnings({"AccessingNonPublicFieldOfAnotherObject", "UnqualifiedStaticUsage"})
public class DataFlowVariable<T> extends DataFlowExpression<T> {
    /**
     * Creates a new unbound Dataflow Variable
     */
    public DataFlowVariable() {
    }

    /**
     * Assigns a value to the variable. Can only be invoked once on each instance of DataFlowVariable
     *
     * @param value The value to assign
     */
    public void leftShift(final T value) {
        bind(value);
    }

    /**
     * Assigns a value from one DataFlowVariable instance to this variable.
     * Can only be invoked once on each instance of DataFlowVariable
     *
     * @param ref The DataFlowVariable instance the value of which to bind
     * @throws InterruptedException If the current thread gets interrupted while waiting for the variable to be bound
     */
    public void leftShift(final DataFlowExpression<T> ref) {
        ref.getValAsync(new MessageStream(){
            public MessageStream send(Object message) {
                bind(ref.value);
                return this;
            }
        });
    }

    @Override
    public Class getRemoteClass() {
        return RemoteDataFlowVariable.class;
    }

    public static class RemoteDataFlowVariable extends DataFlowVariable implements RemoteSerialized {
        private final RemoteHost remoteHost;
        private boolean disconnected;

        public RemoteDataFlowVariable(RemoteHost host) {
            remoteHost = host;
            getValAsync(new MessageStream(){
                public MessageStream send(Object message) {
                    if (!disconnected)
                        remoteHost.write(new BindDataFlow(RemoteDataFlowVariable.this, message, remoteHost.getHostId()));
                    return this;
                }
            });
        }
    }
}
