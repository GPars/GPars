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

package org.gparallelizer;

import groovy.lang.Closure;
import groovy.time.Duration;
import org.gparallelizer.actor.Actor;
import org.gparallelizer.actor.ActorMessage;
import org.gparallelizer.actor.impl.AbstractPooledActor;
import org.gparallelizer.actor.impl.ActorReplyException;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author Alex Tkachman
 */
@SuppressWarnings({"ThrowableInstanceNeverThrown"})
public abstract class ReceivingMessageStream extends MessageStream {
    /**
     * A list of senders for the currently procesed messages
     */
    private final List<MessageStream> senders = new ArrayList<MessageStream>();

    protected final WeakHashMap<Object, MessageStream> obj2Sender = new WeakHashMap<Object, MessageStream>();

    protected final List<MessageStream> getSenders() {
        return senders;
    }

    /**
     * Indicates whether the actor should enhance messages to enable sending replies to their senders
     */
    private volatile boolean sendRepliesFlag = true;

    protected final boolean getSendRepliesFlag() {
        return sendRepliesFlag;
    }

    /**
     * Enabled the actor and received messages to have the reply()/replyIfExists() methods called on them.
     * Sending replies is enabled by default.
     */
    protected final void enableSendingReplies() {
        sendRepliesFlag = true;
    }

    /**
     * Disables the actor and received messages to have the reply()/replyIfExists() methods called on them.
     * Calling reply()/replyIfExist() on the actor will result in IllegalStateException being thrown.
     * Calling reply()/replyIfExist() on a received message will result in MissingMethodException being thrown.
     * Sending replies is enabled by default.
     */
    protected final void disableSendingReplies() {
        sendRepliesFlag = false;
    }

    /**
     * Sends a reply to all currently processed messages. Throws ActorReplyException if some messages
     * have not been sent by an actor. For such cases use replyIfExists().
     * Calling reply()/replyIfExist() on the actor with disabled replying (through the disableSendingReplies() method)
     * will result in IllegalStateException being thrown.
     * Sending replies is enabled by default.
     *
     * @param message reply message
     * @throws org.gparallelizer.actor.impl.ActorReplyException
     *          If some of the replies failed to be sent.
     */
    protected final void reply(final Object message) {
        assert senders != null;
        if (!sendRepliesFlag)
            throw new IllegalStateException("Cannot send a reply $message. Replies have been disabled.");

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
                    exceptions.add(new IllegalArgumentException("Cannot send a reply message " + message + " to a null recipient."));
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
        if (!sendRepliesFlag)
            throw new IllegalStateException("Cannot send a reply $message. Replies have been disabled.");
        for (final MessageStream sender : senders) {
            try {
                if (sender != null)
                    sender.send(message);
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
    protected final Object receive(final Duration duration) throws InterruptedException {
        return receive(duration.toMilliseconds(), TimeUnit.MILLISECONDS);
    }

    public static final class ReplyCategory {
        public static void reply(Object original, Object reply) {
            if (original instanceof ReceivingMessageStream) {
                ((ReceivingMessageStream) original).reply(reply);
                return;
            }

            if (original instanceof Closure) {
                ((ReceivingMessageStream) ((Closure) original).getDelegate()).reply(reply);
                return;
            }

            final AbstractPooledActor actor = (AbstractPooledActor) Actor.threadBoundActor();
            if (actor == null)
                throw new IllegalStateException("reply from non-actor");

            MessageStream sender = actor.obj2Sender.get(original);
            if (sender == null)
                throw new IllegalStateException("Cannot send a reply message " + original.toString() + " to a null recipient.");

            sender.send(reply);
        }

        public static void replyIfExists(Object original, Object reply) {
            if (original instanceof ReceivingMessageStream) {
                ((ReceivingMessageStream) original).replyIfExists(reply);
                return;
            }

            if (original instanceof Closure) {
                ((ReceivingMessageStream) ((Closure) original).getDelegate()).replyIfExists(reply);
                return;
            }

            final AbstractPooledActor actor = (AbstractPooledActor) Actor.threadBoundActor();
            if (actor != null) {
                MessageStream sender = actor.obj2Sender.get(original);
                if (sender != null)
                    try {
                        sender.send(reply);
                    }
                    catch (IllegalStateException e) {//
                    }
            }
        }
    }
}
