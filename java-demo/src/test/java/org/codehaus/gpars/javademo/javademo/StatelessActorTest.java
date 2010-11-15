/*
 * Copyright 2005-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.gpars.javademo.javademo;

import groovyx.gpars.MessagingRunnable;
import groovyx.gpars.actor.DynamicDispatchActor;
import org.junit.Test;


public class StatelessActorTest {
    @Test
    public void testActor() throws InterruptedException {
        final MyStatelessActor actor = new MyStatelessActor();
        actor.start();
        actor.send("Hello");
        actor.sendAndWait(10);
        actor.sendAndContinue(10.0, new MessagingRunnable<String>() {
            @Override
            protected void doRun(final String s) {
                System.out.println("Received a reply " + s);
            }
        });
    }


    private static class MyStatelessActor extends DynamicDispatchActor {
        public void onMessage(final String msg) {
            System.out.println("Received " + msg);
            replyIfExists("Thank you");
        }

        public void onMessage(final Integer msg) {
            System.out.println("Received a number " + msg);
            replyIfExists("Thank you");
        }

        public void onMessage(final Object msg) {
            System.out.println("Received an object " + msg);
            replyIfExists("Thank you");
        }
    }
}