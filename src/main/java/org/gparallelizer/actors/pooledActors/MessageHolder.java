package org.gparallelizer.actors.pooledActors;

import org.gparallelizer.actors.ActorMessage;

import java.util.Arrays;
import java.util.List;
import java.util.Collections;

@SuppressWarnings({"InstanceVariableOfConcreteClass"})
public class MessageHolder {
    private final int capacity;
    private int currentSize = 0;
    private final ActorMessage[] messages;
    private boolean timeout = false;

    MessageHolder(final int capacity) {
        this.capacity = capacity;
        messages = new ActorMessage[capacity];
    }

    public int getCurrentSize() {
        return currentSize;
    }

    public boolean isReady() {
        return timeout || getCurrentSize() == capacity;
    }

    public boolean isTimeout() {
        return timeout;
    }

    public void addMessage(final ActorMessage message) {
        if (isReady()) throw new IllegalStateException("The MessageHolder cannot accept new messages when ready");
        messages[currentSize] = message;
        currentSize++;
        if (message.getPayLoad().equals(ActorException.TIMEOUT)) timeout = true;
    }

    public List<ActorMessage> getMessages() {
        if (!isReady()) throw new IllegalStateException("Cannot build messages before being in the ready state");
        return Collections.unmodifiableList(Arrays.asList(messages));
    }
}