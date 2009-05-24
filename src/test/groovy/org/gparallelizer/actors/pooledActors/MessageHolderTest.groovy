package org.gparallelizer.actors.pooledActors

import org.gparallelizer.actors.ActorMessage

public class MessageHolderTest extends GroovyTestCase {
    public void testHolder() {
        final MessageHolder holder = new MessageHolder(5)
        assertEquals 0, holder.currentSize
        assertFalse holder.ready

        shouldFail(IllegalStateException) {
            holder.messages
        }

        holder.addMessage(createMessage('Message 1'))
        assertEquals 1, holder.currentSize
        assertFalse holder.ready

        shouldFail(IllegalStateException) {
            holder.messages
        }

        holder.addMessage createMessage('Message 2')
        holder.addMessage createMessage('Message 3')
        holder.addMessage createMessage('Message 4')
        holder.addMessage createMessage('Message 5')
        assertEquals 5, holder.currentSize
        assert holder.ready

        shouldFail(IllegalStateException) {
            holder.addMessage createMessage('Message 6')
        }

        List<ActorMessage> messages = holder.messages
        assertEquals 5, messages.size()
        assertEquals(['Message 1', 'Message 2', 'Message 3', 'Message 4', 'Message 5'], messages*.payLoad)
    }

    public void testTimeout() {
        final MessageHolder holder = new MessageHolder(5)
        assertFalse holder.ready
        assertFalse holder.timeout

        holder.addMessage createMessage(ActorException.TIMEOUT)
        assert holder.ready
        assert holder.timeout
    }

    public void testZeroHolder() {
        final MessageHolder holder = new MessageHolder(0)
        assertFalse holder.ready
        assertFalse holder.timeout

        holder.addMessage createMessage('Message 1')
        assert holder.ready
        assertFalse holder.timeout
    }

    public void testZeroHolderTimeout() {
        final MessageHolder holder = new MessageHolder(0)
        assertFalse holder.ready
        assertFalse holder.timeout

        holder.addMessage createMessage(ActorException.TIMEOUT)
        assert holder.ready
        assert holder.timeout
    }

    private final ActorMessage createMessage(Object payLoad) {
        new ActorMessage(payLoad, null)
    }
}