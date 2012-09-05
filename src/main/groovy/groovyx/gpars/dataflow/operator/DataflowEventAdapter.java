// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2012  The original author or authors
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

package groovyx.gpars.dataflow.operator;

import groovyx.gpars.dataflow.DataflowReadChannel;
import groovyx.gpars.dataflow.DataflowWriteChannel;

import java.util.List;

/**
 * A default empty implementation of DataflowEventListener
 *
 * @author Vaclav Pech
 */
public class DataflowEventAdapter implements DataflowEventListener {
    @Override
    public void afterStart(final DataflowProcessor processor) {
    }

    @Override
    public void afterStop(final DataflowProcessor processor) {
    }

    @Override
    public boolean onException(final DataflowProcessor processor, final Throwable e) {
        return false;
    }

    @Override
    public Object messageArrived(final DataflowProcessor processor, final DataflowReadChannel<Object> channel, final int index, final Object message) {
        return message;
    }

    @Override
    public Object controlMessageArrived(final DataflowProcessor processor, final DataflowReadChannel<Object> channel, final int index, final Object message) {
        return message;
    }

    @Override
    public Object messageSentOut(final DataflowProcessor processor, final DataflowWriteChannel<Object> channel, final int index, final Object message) {
        return message;
    }

    @Override
    public List<Object> beforeRun(final DataflowProcessor processor, final List<Object> messages) {
        return messages;
    }

    @Override
    public void afterRun(final DataflowProcessor processor, final List<Object> messages) {
    }

    @Override
    public void customEvent(final DataflowProcessor processor, final Object data) {
    }
}
