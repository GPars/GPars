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

import groovy.lang.Closure;
import groovyx.gpars.actor.Actor;

import java.text.MessageFormat;

/**
 * Enhances objects with the ability to send replies and detect message originators.
 */
@SuppressWarnings({"UtilityClass"})
public final class ReplyCategory {
    private ReplyCategory() {
    }

    /**
     * Retrieves the originator of a message
     *
     * @param original The message to detect the originator of
     * @return The message originator
     */
    public static MessageStream getSender(final Object original) {
        final ReplyingMessageStream actor = Actor.threadBoundActor();
        if (actor == null) {
            throw new IllegalStateException("message originator detection in a non-actor");
        }

        return actor.obj2Sender.get(original);
    }

    public static void reply(final Object original, final Object reply) {
        if (original instanceof ReplyingMessageStream) {
            ((ReplyingMessageStream) original).reply(reply);
            return;
        }

        if (original instanceof Closure) {
            ((ReplyingMessageStream) ((Closure) original).getDelegate()).reply(reply);
            return;
        }

        final ReplyingMessageStream actor = Actor.threadBoundActor();
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
        if (original instanceof ReplyingMessageStream) {
            ((ReplyingMessageStream) original).replyIfExists(reply);
            return;
        }

        if (original instanceof Closure) {
            ((ReplyingMessageStream) ((Closure) original).getDelegate()).replyIfExists(reply);
            return;
        }

        final ReplyingMessageStream actor = Actor.threadBoundActor();
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
