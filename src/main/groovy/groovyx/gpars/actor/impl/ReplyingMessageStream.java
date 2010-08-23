// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.actor.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

/**
 * @author Alex Tkachman, Vaclav Pech
 */
@SuppressWarnings({"ThrowableInstanceNeverThrown"})
public abstract class ReplyingMessageStream extends MessageStream {
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

}
