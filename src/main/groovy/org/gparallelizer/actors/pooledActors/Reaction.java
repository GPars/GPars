//  GParallelizer
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

package org.gparallelizer.actors.pooledActors;

import groovy.lang.Closure;
import org.gparallelizer.actors.ActorMessage;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Buffers messages for the next continuation of an event-driven actor, handles timeouts and no-param continuations.
 *
 * @author Vaclav Pech, Alex Tkachman
 *         Date: May 22, 2009
 */
@SuppressWarnings({"InstanceVariableOfConcreteClass"})
final class Reaction implements Runnable {
    private final int numberOfExpectedMessages;
    private int currentSize = 0;
    private final ActorMessage[] messages;
    private boolean timeout = false;
    private final AbstractPooledActor actor;
    private final Closure code;

    /**
     * Creates a new instance.
     *
     * @param numberOfExpectedMessages The number of messages expected by the next continuation
     * @param code
     */
    Reaction(AbstractPooledActor actor, final int numberOfExpectedMessages, Closure code) {
        this.actor = actor;
        this.code = code;
        this.numberOfExpectedMessages = numberOfExpectedMessages;
        messages = new ActorMessage[numberOfExpectedMessages == 0 ? 1 : numberOfExpectedMessages];
    }

    Reaction(final int numberOfExpectedMessages) {
        this(null, numberOfExpectedMessages, null);
    }

    /**
     * Retrieves the current number of messages in the buffer.
     *
     * @return The curent buffer size
     */
    public int getCurrentSize() {
        return currentSize;
    }

    /**
     * Indicates, whether a timeout message is held in the buffer
     *
     * @return True, if a timeout event has been detected.
     */
    public boolean isTimeout() {
        return timeout;
    }

    /**
     * Indicates whether the buffer contains all the messages required for the next continuation.
     *
     * @return True, if the next continuation can start.
     */
    public boolean isReady() {
        return timeout || (getCurrentSize() == (numberOfExpectedMessages == 0 ? 1 : numberOfExpectedMessages));
    }

    /**
     * Adds a new message to the buffer.
     *
     * @param message The message to add.
     */
    public void addMessage(final ActorMessage message) {
        if (isReady()) throw new IllegalStateException("The MessageHolder cannot accept new messages when ready");
        messages[currentSize] = message;
        currentSize++;
        if (ActorException.TIMEOUT.equals(message.getPayLoad())) timeout = true;
    }

    /**
     * Retrieves messages for the next continuation once the MessageHolder is ready.
     *
     * @return The messages to pass to the next continuation.
     */
    public List<ActorMessage> getMessages() {
        if (!isReady()) throw new IllegalStateException("Cannot build messages before being in the ready state");
        return Collections.unmodifiableList(Arrays.asList(messages));
    }

    /**
     * Dumps so far stored messages. Useful on timeout to restore the already delivered messages
     * to the afterStop() handler in the PooledActor's sweepQueue() method..
     *
     * @return The messages stored so far.
     */
    List<ActorMessage> dumpMessages() {
        return Collections.unmodifiableList(Arrays.asList(messages));
    }

    public void run() {
        actor.runReaction(Arrays.asList(messages), numberOfExpectedMessages, code);
    }
}
