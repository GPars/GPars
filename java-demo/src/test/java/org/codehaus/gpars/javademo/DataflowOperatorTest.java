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

package org.codehaus.gpars.javademo;

import groovyx.gpars.DataflowMessagingRunnable;
import groovyx.gpars.dataflow.Dataflow;
import groovyx.gpars.dataflow.DataflowQueue;
import groovyx.gpars.dataflow.operator.DataflowProcessor;
import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Vaclav Pech, Lukas Krecan, Pavel Jetensky, Michal Franc
 */

@SuppressWarnings({"rawtypes", "RawUseOfParameterizedType", "MagicNumber"})
public class DataflowOperatorTest {
    @Test
    public void testFlow() throws Exception {
        // Simply said, stream is a queue of values, either input or output queue
        final DataflowQueue stream1 = new DataflowQueue();
        final DataflowQueue stream2 = new DataflowQueue();
        final DataflowQueue stream3 = new DataflowQueue();
        final DataflowQueue stream4 = new DataflowQueue();

        // processor1 waits for value in stream 1 and writes 2*value to stream2
        final List<DataflowQueue> calculationInputList = asList(stream1);
        final List<DataflowQueue> calculationOutputList = asList(stream2);
        final DataflowProcessor processor1 = Dataflow.operator(calculationInputList, calculationOutputList, new DataflowMessagingRunnable(1) {
            @Override
            protected void doRun(final Object[] objects) {
                // Passing calculated value to output stream
                getOwningProcessor().bindOutput(2 * (Integer) objects[0]);
            }
        });

        assertFalse("Stream2 should not be bound as no value was passed to stream1", stream2.isBound());
        // send values to streams
        stream1.bind(1);


        waitForValue(stream2);
        assertTrue("Stream2 should be bound as value has been calculated by processor1", stream2.isBound());

        // processor2 reads value from stream2 and stream 2 and writes sum to stream 4
        final DataflowProcessor processor2 = Dataflow.operator(asList(stream2, stream3), asList(stream4), new DataflowMessagingRunnable(2) {
            @Override
            protected void doRun(final Object[] objects) {
                getOwningProcessor().bindOutput((Integer) objects[0] + (Integer) objects[1]);
            }
        });

        // Multiple values can be send to one stream
        stream1.bind(2);
        stream1.bind(3);

        assertFalse("Stream3 should not be bound as no value was set and it is input for calculation, not output", stream3.isBound());
        assertFalse("Stream4 should not be bound as no value was passed yet to its input streams", stream4.isBound());
        // processor processor2 waits for stream3 values, lets send them
        stream3.bind(100);
        stream3.bind(100);
        stream3.bind(100);
        waitForValue(stream4);
        assertTrue("Stream4 should be bound as values has been passed to its input streams", stream4.isBound());

        //fetch values
        assertEquals(102, stream4.getVal());
        assertEquals(104, stream4.getVal());
        assertEquals(106, stream4.getVal());

        assertFalse("All values fetched, no output expected", stream4.isBound());

        // would wait for another input and hang
        //  stream4.getVal();

        processor1.stop();
        processor2.stop();
    }

    /**
     * Waits for value to appear in stream.
     *
     * @param stream The stream to try to read from
     * @throws InterruptedException If the thread gets interrupted while sleeping
     */
    private static void waitForValue(final DataflowQueue<?> stream) throws InterruptedException {
        while (!stream.isBound()) {
            Thread.sleep(100L);
        }
    }
}

