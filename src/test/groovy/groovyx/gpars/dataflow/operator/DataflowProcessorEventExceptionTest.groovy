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

package groovyx.gpars.dataflow.operator

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.group.PGroup

import java.util.concurrent.CopyOnWriteArrayList

/**
 * @author Vaclav Pech
 * Date: Sep 9, 2009
 */

public class DataflowProcessorEventExceptionTest extends GroovyTestCase {

    private PGroup group
    final DataflowQueue a = new DataflowQueue()
    final DataflowQueue b = new DataflowQueue()
    final DataflowQueue c = new DataflowQueue()

    protected void setUp() {
        group = new DefaultPGroup(1)
        super.setUp()
    }

    protected void tearDown() {
        group.shutdown()
        super.tearDown()
    }

    public void testNoListeners() {
        def op = group.operator(inputs: [a, b], outputs: [c]) {x, y ->
            if (x == 1) throw new IllegalArgumentException('test')
            bindOutput x + y
        }

        a << 10
        b << 20
        assert 30 == c.val

        a << 1
        b << 2
        sleep 500
        assert !c.bound
        op.join()
    }

    public void testErrorIgnoringListener() {
        final listener1 = new DataflowEventAdapter() {
            @Override
            boolean onException(final DataflowProcessor processor, final Throwable e) {
                return false
            }
        }
        def op = group.operator(inputs: [a, b], outputs: [c], listeners: [listener1]) {x, y ->
            if (x == 1) throw new IllegalArgumentException('test')
            bindOutput x + y
        }

        a << 10
        b << 20
        assert 30 == c.val

        a << 1
        b << 2
        assert 3 == c.val

        op.terminate()
    }

    class ExceptionTestListener extends DataflowEventAdapter {
        volatile CopyOnWriteArrayList<String> events = []

        List<String> retrieveEvents(Closure filter) {
            events.findAll filter
        }

        int countEventsThatStartWith(String filter) {
            retrieveEvents {it.startsWith(filter)}.size()
        }

        @Override
        void afterStop(final DataflowProcessor processor) {
            events << "afterStop"
        }

        @Override
        boolean onException(final DataflowProcessor processor, final Throwable e) {
            events << "onException"
            false
        }

        @Override
        void afterRun(final DataflowProcessor processor, final List<Object> messages) {
            events << "afterRun"
        }

        @Override
        void customEvent(final DataflowProcessor processor, final Object data) {
            events << "customEvent:" + data
        }
    }
}
