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

package org.codehaus.gpars.javademo.javademo;

import groovy.lang.Closure;
import groovyx.gpars.ReactorMessagingRunnable;
import groovyx.gpars.actor.Actor;
import groovyx.gpars.actor.Actors;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ReactorTest {

    @Test
    public void testReactor() throws Exception {

        // Simple message handler reacts on message by multiplying it
        final Closure messageHandler = new ReactorMessagingRunnable<Integer, Integer>() {
            @Override
            protected Integer doRun(final Integer integer) {
                return integer * 2;
            }
        };
        final Actor actor = Actors.reactor(messageHandler);

        assertEquals("Result is not matching", 2, actor.sendAndWait(1));
        assertEquals("Result is not matching", 4, actor.sendAndWait(2));
        assertEquals("Result is not matching", 6, actor.sendAndWait(3));
    }
}

