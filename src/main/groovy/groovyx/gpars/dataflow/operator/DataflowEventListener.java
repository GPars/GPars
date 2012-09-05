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
 * Enables external entities to observe the state of dataflow selectors and operators
 *
 * @author Vaclav Pech
 */
public interface DataflowEventListener {
    void afterStart(DataflowProcessor processor);

    void afterStop(DataflowProcessor processor);

    boolean onException(DataflowProcessor processor, Throwable e);

    Object messageArrived(DataflowProcessor processor, DataflowReadChannel<Object> channel, int index, Object message);

    Object controlMessageArrived(DataflowProcessor processor, DataflowReadChannel<Object> channel, int index, Object message);

    Object messageSentOut(DataflowProcessor processor, DataflowWriteChannel<Object> channel, int index, Object message);

    List<Object> beforeRun(DataflowProcessor processor, List<Object> messages);

    void afterRun(DataflowProcessor processor, List<Object> messages);

    void customEvent(DataflowProcessor processor, Object data);
}
