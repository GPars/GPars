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
import groovyx.gpars.actor.Actor;
import groovyx.gpars.actor.DefaultActor;
import org.junit.Test;

/**
 * Test shows how reacting to messages sent to actor can be nested in logical chain
 *
 * @author Lukas Krecan
 */
public class StatefulActorTest {
    @Test
    public void testStatefulActor() throws Exception {
        final Actor actor = new MyActor();
        actor.start();
        actor.send("Hello");
        actor.send("Hello again");
        actor.send(10);

        actor.send("Bye");
        actor.send("Bye again");
        actor.send(10);

        Thread.sleep(2000);

    }

    private static class MyActor extends DefaultActor {

        @Override
        protected void act() {

            loop(new Runnable() {

                // Run will be executed with first message sent to actor
                @Override
                public void run() {

                    // Schedule process to retrieve second message from queue and MessagingRunnable to process it
                    react(new MessagingRunnable<Object>(this) {
                        @Override
                        protected void doRun(final Object s) {
                            System.out.println("Received in react: " + s);

                            // Schedule process to retrieve third message from queue and MessagingRunnable to process it
                            react(new MessagingRunnable<String>() {
                                @Override
                                protected void doRun(final String s) {
                                    System.out.println("Received in nested react: " + s);

                                    react(new MessagingRunnable<Integer>() {
                                        @Override
                                        protected void doRun(final Integer integer) {
                                            System.out.println("Received a number in nested react: " + integer);
                                        }
                                    });
                                }
                            });
                        }
                    });
                }
            });
        }
    }
}