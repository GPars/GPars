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

package org.gparallelizer.actors

import org.gparallelizer.actors.pooledActors.ActorReplyException
import org.gparallelizer.actors.pooledActors.PooledActorGroup
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit;

/**
 * Represents the common superclass to both thread-bound and event-driven actors.
 *
 * @author Vaclav Pech
 * Date: Jun 13, 2009
 */
public abstract class CommonActorImpl implements Actor {

    /**
     * A list of senders for the currently procesed messages
     */
    protected final List senders = []

    //todo necessary for mixins
    protected final List getSenders() {senders}

    /**
     * Indicates whether the actor should enhance messages to enable sending replies to their senders
     */
    protected volatile boolean sendRepliesFlag = true

    //todo necessary for mixins
    protected final boolean getSendRepliesFlag() {sendRepliesFlag}

    /**
     * Enabled the actor and received messages to have the reply()/replyIfExists() methods called on them.
     * Sending replies is enabled by default.
     */
    protected final void enableSendingReplies() { sendRepliesFlag = true }

    /**
     * Disables the actor and received messages to have the reply()/replyIfExists() methods called on them.
     * Calling reply()/replyIfExist() on the actor will result in IllegalStateException being thrown.
     * Calling reply()/replyIfExist() on a received message will result in MissingMethodException being thrown.
     * Sending replies is enabled by default.
     */
    protected final void disableSendingReplies() { sendRepliesFlag = false }

    /**
     * The actor group to which the actor belongs
     */
    volatile AbstractActorGroup actorGroup

    /**
     * Indicates whether the actor's group can be changed. It is typically not changeable after actor starts.
     */
    private volatile boolean groupMembershipChangeable = true

    /**
     * Disallows any subsequent changes to the group attached to the actor.
     */
    protected final void disableGroupMembershipChange() { groupMembershipChangeable = false }

    /**
     * Sets the actor's group.
     * It can only be invoked before the actor is started.
     */
    public final void setActorGroup(def group) {
        if (!groupMembershipChangeable) throw new IllegalStateException("Cannot set actor's group on a started actor.")
        if (!group) throw new IllegalArgumentException("Cannot set actor's group to null.")
        actorGroup = group
    }

    /**
     * Gets unblocked after the actor stops.
     */
    private final CountDownLatch joinLatch = new CountDownLatch(1)
    protected final CountDownLatch getJoinLatch() { joinLatch }

    /**
     * Joins the actor. Waits fot its termination.
     */
    public final void join() { join(0) }

    /**
     * Joins the actor. Waits fot its termination.
     * @param milis Timeout in miliseconds, specifying how long to wait at most.
     */
    public final void join(long milis) {
        if (milis > 0) joinLatch.await(milis, TimeUnit.MILLISECONDS)
        else joinLatch.await()
    }

    /**
     * Sends a reply to all currently processed messages. Throws ActorReplyException if some messages
     * have not been sent by an actor. For such cases use replyIfExists().
     * Calling reply()/replyIfExist() on the actor with disabled replying (through the disableSendingReplies() method)
     * will result in IllegalStateException being thrown.
     * Sending replies is enabled by default.
     * @throws ActorReplyException If some of the replies failed to be sent.
     */
    protected final void reply(Object message) {
        assert senders != null
        if (!sendRepliesFlag) throw new IllegalStateException("Cannot send a reply $message. Replies have been disabled.")
        if (!senders.isEmpty()) {
            List<Exception> exceptions = []
            for (sender in senders) {
                if (sender != null) {
                    try { sender.send message } catch (IllegalStateException e) {exceptions << e }
                }
                else exceptions << new IllegalArgumentException("Cannot send a reply message ${message} to a null recipient.")
            }
            if (!exceptions.empty) throw new ActorReplyException('Failed sending some replies. See the issues field for details', exceptions)
        } else {
            throw new ActorReplyException("Cannot send replies. The list of recipients is empty.")
        }
    }

    /**
     * Sends a reply to all currently processed messages, which have been sent by an actor.
     * Ignores potential errors when sending the replies, like no sender or sender already stopped.
     * Calling reply()/replyIfExist() on the actor with disabled replying (through the disableSendingReplies() method)
     * will result in IllegalStateException being thrown.
     * Sending replies is enabled by default.
     */
    protected final void replyIfExists(Object message) {
        assert senders != null
        if (!sendRepliesFlag) throw new IllegalStateException("Cannot send a reply $message. Replies have been disabled.")
        for (sender in senders) {
            try {
                sender?.send message
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
                def replier = message.payLoad
                def sender = message.sender
                replier?.getMetaClass()?.reply = {msg ->
                    if (sender != null) sender.send msg
                    else throw new IllegalArgumentException("Cannot send a reply message ${msg} to a null recipient.")
                }

                replier?.getMetaClass()?.replyIfExists = {msg ->
                    try {
                        sender?.send msg
                    } catch (IllegalStateException ignore) { }
                }
            }
        }
    }
}
