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

package org.gparallelizer.actors;

import groovy.lang.Closure;
import groovy.time.Duration;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.GeneratedClosure;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.gparallelizer.MessageStream;
import org.gparallelizer.actors.pooledActors.ActorReplyException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Represents the common superclass to both thread-bound and event-driven actors.
 *
 * @author Vaclav Pech, Alex Tkachman
 * Date: Jun 13, 2009
 */
public abstract class CommonActorImpl extends Actor {

    /**
     * A list of senders for the currently procesed messages
     */
    private final List<MessageStream> senders = new ArrayList<MessageStream>();
    private static final String RECEIVE_IMPL_METHOD_SHOULD_BE_IMPLEMENTED = "'receiveImpl' method should be implemented by subclass of MessageStream";

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
     * The actor group to which the actor belongs
     */
    //todo ensure proper serialization
    private volatile AbstractActorGroup actorGroup;

    /**
     * Indicates whether the actor's group can be changed. It is typically not changeable after actor starts.
     */
    private volatile boolean groupMembershipChangeable = true;

    /**
     * Disallows any subsequent changes to the group attached to the actor.
     */
    protected final void disableGroupMembershipChange() { groupMembershipChangeable = false; }

    /**
     * Sets the actor's group.
     * It can only be invoked before the actor is started.
     * @param group new group
     */
    public final void setActorGroup(final AbstractActorGroup group) {
        if (!groupMembershipChangeable)
            throw new IllegalStateException("Cannot set actor's group on a started actor.");

        if (group == null)
            throw new IllegalArgumentException("Cannot set actor's group to null.");

        actorGroup = group;
    }

    /**
     * Gets unblocked after the actor stops.
     */
    //todo ensure proper serialization
    private final CountDownLatch joinLatch = new CountDownLatch(1);
    protected final CountDownLatch getJoinLatch() { return joinLatch; }

    /**
     * Joins the actor. Waits fot its termination.
     */
    @Override
    public final void join() throws InterruptedException {
        join(0);
    }

    /**
     * Joins the actor. Waits fot its termination.
     * @param milis Timeout in miliseconds, specifying how long to wait at most.
     */
    @Override
    public final void join(final long milis) throws InterruptedException {
        if (milis > 0)
            joinLatch.await(milis, TimeUnit.MILLISECONDS);
        else
            joinLatch.await();
    }

    /**
     * Sends a reply to all currently processed messages. Throws ActorReplyException if some messages
     * have not been sent by an actor. For such cases use replyIfExists().
     * Calling reply()/replyIfExist() on the actor with disabled replying (through the disableSendingReplies() method)
     * will result in IllegalStateException being thrown.
     * Sending replies is enabled by default.
     * @param message reply message
     * @throws ActorReplyException If some of the replies failed to be sent.
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
                    exceptions.add(new IllegalArgumentException("Cannot send a reply message ${message} to a null recipient."));
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
     * @param message reply message
     */
    protected final void replyIfExists(final Object message) {
        assert senders != null;
        if (!sendRepliesFlag)
            throw new IllegalStateException("Cannot send a reply $message. Replies have been disabled.");
        for (final MessageStream sender : senders) {
            try {
                if (sender != null)
                    sender.send (message);
            } catch (IllegalStateException ignore) { }
        }
    }

    /**
     * Adds reply() and replyIfExists() methods to all the messages.
     * These methods will call send() on the target actor (the sender of the original message).
     * Calling reply()/replyIfExist() on messages from within an actor with disabled replying (through the disableSendingReplies()
     * method) may result in unexpected behavior. The MissingMethodException may be thrown from reply()/replyIfExists(),
     * however, if the messages have been received by a different actor, with enabled replying) before
     * and then reused, the reply()/replyIfExists() methods on the message would reply to that actor,
     * not the immediate sender of the message.
     * Sending replies is enabled by default.

     * @param messages The instance of ActorMessage wrapping the sender actor, which we need to be able to respond to,
     * plus the original message
     */
    protected final void enhanceWithReplyMethodsToMessages(final List<ActorMessage> messages) {
        for (final ActorMessage message: messages) {
            if (message != null) {
                //Enhances the replier's metaClass with reply() and replyIfExists() methods to send messages to the sender
                final Object replier = message.getPayLoad();
                final MessageStream sender = message.getSender();

                if (replier != null) {
                    Object mc = DefaultGroovyMethods.getMetaClass(replier);
                    if (mc != null) {
                        InvokerHelper.setProperty(mc, "reply", new MyClosure(sender, true));
                        mc = DefaultGroovyMethods.getMetaClass(replier);
                        InvokerHelper.setProperty(mc, "replyIfExists", new MyClosure(sender, false));
                    }
                }
            }
        }
    }

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     * @return The message retrieved from the queue, or null, if the timeout expires.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    protected final Object receive () throws InterruptedException {
        final Object msg = receiveImpl();
        if (msg instanceof ActorMessage) {
            final ActorMessage messageAndReply = (ActorMessage) msg;
            return messageAndReply.getPayLoad();
        }
        else {
            return msg;
        }
    }

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     * @param timeout how long to wait before giving up, in units of unit
     * @param units a TimeUnit determining how to interpret the timeout parameter
     * @return The message retrieved from the queue, or null, if the timeout expires.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    protected final Object receive (final long timeout, final TimeUnit units) throws InterruptedException {
        final Object msg = receiveImpl(timeout, units);
        if (msg instanceof ActorMessage) {
            final ActorMessage messageAndReply = (ActorMessage) msg;
            return messageAndReply.getPayLoad();
        }
        else {
            return msg;
        }
    }

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     * @return The message retrieved from the queue, or null, if the timeout expires.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    protected Object receiveImpl () throws InterruptedException {
        throw new UnsupportedOperationException(RECEIVE_IMPL_METHOD_SHOULD_BE_IMPLEMENTED);
    }

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     * @param timeout how long to wait before giving up, in units of unit
     * @param units a TimeUnit determining how to interpret the timeout parameter
     * @return The message retrieved from the queue, or null, if the timeout expires.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    protected Object receiveImpl(final long timeout, final TimeUnit units) throws InterruptedException {
        throw new UnsupportedOperationException(RECEIVE_IMPL_METHOD_SHOULD_BE_IMPLEMENTED);
    }

    /**
     * Retrieves a message from the message queue, waiting, if necessary, for a message to arrive.
     * @param duration how long to wait before giving up, in units of unit
     * @return The message retrieved from the queue, or null, if the timeout expires.
     * @throws InterruptedException If the thread is interrupted during the wait. Should propagate up to stop the thread.
     */
    protected final Object receive(final Duration duration) throws InterruptedException {
        return receive(duration.toMilliseconds(), TimeUnit.MILLISECONDS);
    }

    /**
     * Retrieves the group to which the actor belongs
     * @return The actor's group
     */
    public AbstractActorGroup getActorGroup() {
        return actorGroup;
    }

    /**
     * Specifies a reply handler on messages. Remembers the original sender and a flag, whether failed delivery
     * should be treated as an failure or not.
     */
    private static class MyClosure extends Closure implements GeneratedClosure {
        private final MessageStream sender;
        private final boolean throwable;

        private MyClosure(final MessageStream sender, final boolean throwable) {
            super(null, null);
            this.sender = sender;
            this.throwable = throwable;
        }

        @SuppressWarnings({"UnusedDeclaration"})
        public Object doCall() {
            return doCall(null);
        }

        public Object doCall(final Object msg) {
            if (throwable) {
                if (sender != null)
                    return sender.send(msg);
                else
                      throw new IllegalArgumentException("Cannot send a reply message " + msg.toString() + " to a null recipient.");
            }
            else {
                try {
                    if (sender != null)
                        return sender.send(msg);
                    else
                        return null;
                }
                catch (IllegalStateException ignored) {
                    return null;
                }
            }
        }
    }
}
