package org.gparallelizer.actors.pooledActors;

import org.gparallelizer.actors.ActorMessage;

import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import static java.lang.Math.*;

/**
 *
 * @author Vaclav Pech
 * Date: May 22, 2009
 */
@SuppressWarnings({"InstanceVariableOfConcreteClass"})
public final class MessageHolder {
    private final int numberOfExpectedMessages;
    private int currentSize = 0;
    private final ActorMessage[] messages;
    private boolean timeout = false;

    MessageHolder(final int numberOfExpectedMessages) {
        this.numberOfExpectedMessages = max(1, numberOfExpectedMessages);  //the numberOfExpectedMessages field cannot be zero
        messages = new ActorMessage[numberOfExpectedMessages];  //the array can have zero size
    }

    public int getCurrentSize() { return currentSize; }

    public boolean isTimeout() { return timeout; }

    public boolean isReady() {
        return timeout || getCurrentSize() == numberOfExpectedMessages;
    }

    public void addMessage(final ActorMessage message) {
        if (isReady()) throw new IllegalStateException("The MessageHolder cannot accept new messages when ready");
        if (canHoldMessages()) messages[currentSize] = message;
        currentSize++;
        if (message.getPayLoad().equals(ActorException.TIMEOUT)) timeout = true;
    }

    public List<ActorMessage> getMessages() {
        if (!isReady()) throw new IllegalStateException("Cannot build messages before being in the ready state");
        return Collections.unmodifiableList(Arrays.asList(messages));
    }

    private boolean canHoldMessages() { return messages.length > 0; }
}