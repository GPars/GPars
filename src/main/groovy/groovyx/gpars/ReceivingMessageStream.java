//  GPars (formerly GParallelizer)
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

package groovyx.gpars;

import groovy.lang.Closure;
import groovy.time.BaseDuration;
import groovyx.gpars.actor.Actor;
import groovyx.gpars.actor.ActorMessage;
import groovyx.gpars.actor.impl.ActorReplyException;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author Alex Tkachman, Vaclav Pech
 */
@SuppressWarnings({"ThrowableInstanceNeverThrown"})
public abstract class ReceivingMessageStream extends MessageStream {
    /**
     * A list of senders for the currently processed messages
     */
    private final List<MessageStream> senders = new ArrayList<MessageStream>();

    protected final WeakHashMap<Object, MessageStream> obj2Sender = new WeakHashMap<Object, MessageStream>();

    @SuppressWarnings({"ReturnOfCollectionOrArrayField"})
    protected final List<MessageStream> getSenders() {
        return senders;
    }

    /**
     * Sends a reply to all currently processed messages. Throws ActorReplyException if some messages
     * have not been sent by an actor. For such cases use replyIfExists().
     * Calling reply()/replyIfExist() on the actor with disabled replying (through the disableSendingReplies() method)
     * will result in IllegalStateException being thrown.
     * Sending replies is enabled by default.
     *
     * @param message reply message
     * @throws groovyx.gpars.actor.impl.ActorReplyException
     *          If some of the replies failed to be sent.
     */
    protected final void reply(final Object message) {
        assert senders != null;
        if (senders.isEmpty()) {
            throw new ActorReplyException("Cannot send replies. The list of recipients is empty.");
        } else {
            final List<Exception> exceptions = new ArrayList<Exception>();
            for (final MessageStream sender : senders) {
                if (sender != null) {
                    try {
                        sender.send(message);
                    }
                    catch (IllegalStateException e) {
                        exceptions.add(e);
                    }
                } else {
                    exceptions.add(new IllegalArgumentException(String.format("Cannot send a reply message %s to a null recipient.", message)));
                }
            }
            if (!exceptions.isEmpty()) {
                throw new ActorReplyException("Failed sending some replies. See the issues field for details", exceptions);
            }
        }
    }

    /**
     * Sends a reply to all currently processed messages, which have been sent by an actor.
     * Ignores potential errors when sending the replies, like no sender or sender already stopped.
     * Calling reply()/replyIfExist() on the actor with disabled replying (through the disableSendingReplies() method)
     * will result in IllegalStateException being thrown.
     * Sending replies is enabled by default.
     *
     * @param message reply message
     */
    protected final void replyIfExists(final Object message) {
        assert senders != null;
        for (final MessageStream sender : senders) {
            try {
                if (sender != null) {
                    sender.send(message);
                }
            } catch (IllegalStateException ignore) {
            }
        }
    }

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     *
     * @return The message retrieved from the queue, or null, if the timeout expires.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    protected abstract Object receiveImpl() throws InterruptedException;

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     *
     * @param timeout how long to wait before giving up, in units of unit
     * @param units   a TimeUnit determining how to interpret the timeout parameter
     * @return The message retrieved from the queue, or null, if the timeout expires.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    protected abstract Object receiveImpl(final long timeout, final TimeUnit units) throws InterruptedException;

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     *
     * @return The message retrieved from the queue, or null, if the timeout expires.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    protected final Object receive() throws InterruptedException {
        final Object msg = receiveImpl();
        if (msg instanceof ActorMessage) {
            final ActorMessage messageAndReply = (ActorMessage) msg;
            return messageAndReply.getPayLoad();
        } else {
            return msg;
        }
    }

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     *
     * @param timeout how long to wait before giving up, in units of unit
     * @param units   a TimeUnit determining how to interpret the timeout parameter
     * @return The message retrieved from the queue, or null, if the timeout expires.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    protected final Object receive(final long timeout, final TimeUnit units) throws InterruptedException {
        final Object msg = receiveImpl(timeout, units);
        if (msg instanceof ActorMessage) {
            final ActorMessage messageAndReply = (ActorMessage) msg;
            return messageAndReply.getPayLoad();
        } else {
            return msg;
        }
    }

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     *
     * @param duration how long to wait before giving up, in units of unit
     * @return The message retrieved from the queue, or null, if the timeout expires.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    protected final Object receive(final BaseDuration duration) throws InterruptedException {
        return receive(duration.toMilliseconds(), TimeUnit.MILLISECONDS);
    }

    /**
     * Enhances objects with the ability to send replies and detect message originators.
     */
    public static final class ReplyCategory {
        private ReplyCategory() { }

        /**
         * Retrieves the originator of a message
         *
         * @param original The message to detect the originator of
         * @return The message originator
         */
        public static MessageStream getSender(final Object original) {
            final ReceivingMessageStream actor = Actor.threadBoundActor();
            if (actor == null) {
                throw new IllegalStateException("message originator detection in a non-actor");
            }

            return actor.obj2Sender.get(original);
        }

        public static void reply(final Object original, final Object reply) {
            if (original instanceof ReceivingMessageStream) {
                ((ReceivingMessageStream) original).reply(reply);
                return;
            }

            if (original instanceof Closure) {
                ((ReceivingMessageStream) ((Closure) original).getDelegate()).reply(reply);
                return;
            }

            final ReceivingMessageStream actor = Actor.threadBoundActor();
            if (actor == null) {
                throw new IllegalStateException("reply from non-actor");
            }

            final MessageStream sender = actor.obj2Sender.get(original);
            if (sender == null) {
                throw new IllegalStateException(MessageFormat.format("Cannot send a reply message {0} to a null recipient.", original.toString()));
            }

            sender.send(reply);
        }

        public static void replyIfExists(final Object original, final Object reply) {
            if (original instanceof ReceivingMessageStream) {
                ((ReceivingMessageStream) original).replyIfExists(reply);
                return;
            }

            if (original instanceof Closure) {
                ((ReceivingMessageStream) ((Closure) original).getDelegate()).replyIfExists(reply);
                return;
            }

            final ReceivingMessageStream actor = Actor.threadBoundActor();
            if (actor != null) {
                final MessageStream sender = actor.obj2Sender.get(original);
                if (sender != null) {
                    try {
                        sender.send(reply);
                    } catch (IllegalStateException ignored) {
                    }
                }
            }
    }
  }
}
