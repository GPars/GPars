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

import groovyx.gpars.MessageStream;
import groovyx.gpars.remote.RemoteHost;
import groovyx.gpars.serial.RemoteSerialized;
import groovyx.gpars.serial.SerialContext;

/**
 * @author Alex Tkachman
 */
public class RemoteDataFlowExpression extends DataFlowExpression implements RemoteSerialized {
    private final RemoteHost remoteHost;

    public RemoteDataFlowExpression() {
        remoteHost = (RemoteHost) SerialContext.get();
        getValAsync(new MessageStream() {
            public MessageStream send(Object message) {
                remoteHost.write(new BindDataFlow(RemoteDataFlowExpression.this, message, remoteHost.getHostId()));
                return this;
            }
        });
    }

    protected Object evaluate() {
        return value;
    }

    protected void subscribe(DataFlowExpression.DataFlowExpressionsCollector listener) {
        listener.subscribe(this);
    }
}
