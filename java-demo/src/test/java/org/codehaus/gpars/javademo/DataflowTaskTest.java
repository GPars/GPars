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

package org.codehaus.gpars.javademo;

import groovyx.gpars.MessagingRunnable;
import groovyx.gpars.dataflow.DataflowVariable;
import groovyx.gpars.dataflow.Promise;
import groovyx.gpars.group.DefaultPGroup;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;

@SuppressWarnings({"MagicNumber"})
public class DataflowTaskTest {

    @Test
    public void testDataflowVariable() throws Throwable {
        final List<String> logMessages = new ArrayList<String>();

        final DefaultPGroup group = new DefaultPGroup(10);

        // variable can be assigned once only, read allowed multiple times
        final DataflowVariable<Integer> a = new DataflowVariable<Integer>();

        // group.task will use thread from pool and uses it to execute value bind
        group.task(new Runnable() {
            public void run() {
                // first thread binding value succeeds, other attempts would fail with IllegalStateException
                logMessages.add("Value bound");
                a.bind(10);
            }
        });

        // group.task will use thread from pool and uses it to execute call method
        final Promise<?> result = group.task(new Callable() {
            public Object call() throws Exception {
                // getVal will wait for the value to be assigned
                final int result = a.getVal() + 10;
                logMessages.add("Value calculated");
                return result;
            }
        });

        // Specify, what will happen when value is bound
        result.whenBound(new MessagingRunnable<Integer>() {
            @Override
            protected void doRun(final Integer resultValue) {
                logMessages.add("Result calculated");
                assertEquals("Result value invalid", 20L, resultValue.intValue());
                assertEquals("Wrong order of calls", Arrays.asList("Value bound", "Value calculated", "Result calculated"), logMessages);
            }
        });


        System.out.println("result = " + result.get());
    }
}

