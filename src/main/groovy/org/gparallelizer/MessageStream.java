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

import groovy.time.Duration;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import org.gparallelizer.actors.ActorMessage;
import org.gparallelizer.remote.serial.WithSerialId;
import org.gparallelizer.remote.RemoteMessageStream;

/**
 * Stream of abstract messages
 *
 * @author Alex Tkachman
 */
public abstract class MessageStream extends WithSerialId {
    /**
     * Send message to stream and return immediately
     *
     * @param message message to send
     * @return always return message stream itself
     */
    public abstract MessageStream send (Object message);

    public final <T> MessageStream send (T message, MessageStream replyTo) {
        return send (new ActorMessage<T>(message, replyTo));
    }

    /**
     * Same as send
     * @param message to send
     * @return original stream
     */
    public final <T> MessageStream leftShift (T message) {
        return send(message);
    }

    /**
     * Sends a message and waits for a reply.
     * Returns the reply or throws an IllegalStateException, if the target actor cannot reply.
     * @param message message to send
     * @return The message that came in reply to the original send.
     * @throws InterruptedException if interrupted while waiting
     */
    public final <T> Object sendAndWait (T message) throws InterruptedException {
        ReplyWaiter to = new ReplyWaiter();
        send (new ActorMessage<T>(message, to));
        return to.getResult();
    }

    /**
     * Sends a message and waits for a reply. Timeouts after the specified timeout. In case of timeout returns null.
     * Returns the reply or throws an IllegalStateException, if the target actor cannot reply.
     * @param message message to send
     * @param timeout timeout
     * @param units units
     * @return The message that came in reply to the original send.
     * @throws InterruptedException if interrupted while waiting
     */
    public final <T> Object sendAndWait (long timeout, TimeUnit units, T message) throws InterruptedException {
        ReplyWaiter to = new ReplyWaiter();
        send (new ActorMessage<T>(message, to));
        return to.getResult(timeout, units);
    }

    /**
     * Sends a message and waits for a reply. Timeouts after the specified timeout. In case of timeout returns null.
     * Returns the reply or throws an IllegalStateException, if the target actor cannot reply.
     * @param duration timeout
     * @param message  message to send
     * @return The message that came in reply to the original send.
     * @throws InterruptedException if interrupted while waioting
     */
    public final <T> Object sendAndWait(Duration duration, T message) throws InterruptedException {
        return sendAndWait(duration.toMilliseconds(), TimeUnit.MILLISECONDS, message);
    }

    public Class getRemoteClass() {
        return RemoteMessageStream.class;
    }

    private static class ReplyWaiter extends MessageStream {
        private volatile Object value;

        private volatile boolean isSet;

        private ReplyWaiter() {
            value = Thread.currentThread();
        }

        public MessageStream send(Object message) {
            Thread thread = (Thread) this.value;
            if (message instanceof ActorMessage)
                this.value = ((ActorMessage)message).getPayLoad();
            else
                this.value = message;
            isSet = true;
            LockSupport.unpark(thread);
            return this;
        }

        public Object getResult () throws InterruptedException {
            Thread thread = Thread.currentThread();
            while (!isSet) {
                LockSupport.park();
                if (thread.isInterrupted())
                    throw new InterruptedException();
            }
            if (value instanceof Throwable) {
                if (value instanceof RuntimeException)
                    throw (RuntimeException) value;
                else
                    throw new RuntimeException((Throwable) value);
            }
            return value;
        }

        public Object getResult(long timeout, TimeUnit units) throws InterruptedException {
            long endNano = System.nanoTime() + units.toNanos(timeout);
            Thread thread = Thread.currentThread();
            while (!isSet) {
                long toWait = endNano - System.nanoTime();
                if (toWait <= 0) {
                    return null;
                }
                LockSupport.parkNanos(toWait);
                if (thread.isInterrupted())
                    throw new InterruptedException();
            }
            return value;
        }

        public void onDeliveryError () {
            send(new IllegalStateException("Delivery error. Maybe target actor is not active"));
        }
    }
}
