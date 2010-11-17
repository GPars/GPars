// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
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
import groovyx.gpars.agent.Agent;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vaclav Pech, Lukas Krecan, Pavel Jetensky, Michal Franc
 */
public class AgentTest {

    @Test
    public void testAgent() throws Exception {

        // Agent is thread safe way to store value
        final Agent<Integer> counter = new Agent<Integer>(0);
        counter.send(10);

        assertEquals("Stored agent variable not matching", 10, counter.getVal().intValue());

        // Send command to modify agent value
        counter.send(new MessagingRunnable<Integer>() {
            @Override
            protected void doRun(final Integer value) {
                //we have thread-safe access to the value here
                counter.updateValue(value + 1);
            }
        });

        assertEquals("Final stored agent value not matching", 11, counter.getVal().intValue());
        System.out.println("Current value: " + counter.getVal());
    }
}

