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

import org.gparallelizer.MessageStream;
import org.gparallelizer.remote.RemoteHost;
import org.gparallelizer.remote.RemoteConnection;
import org.gparallelizer.remote.messages.AbstractMsg;
import org.gparallelizer.remote.serial.RemoteSerialized;

/**
 * Actors are active objects, which either have their own thread processing repeatedly messages submitted to them
 * or they borrow a thread from a thread pool. Actors implementing the ThreadedActor interface have their own thread,
 * whereas actors implementing PooledActor interface delegate processing to threads from a thread pool.
 * The Actor interface provides means to send messages to the actor, start and stop the background thread as well as
 * check its status.
 *
 * @author Vaclav Pech, Alex Tkachman
 */
public abstract class Actor extends MessageStream {

    /**
     * Starts the Actor. No messages can be send or received before an Actor is started.
     * @return same actor
     */
    public abstract Actor start();

    /**
     * Stops the Actor. Unprocessed messages will be passed to the afterStop method, if exists.
     * Has no effect if the Actor is not started.
     * @return same actor
     */
    public abstract Actor stop();

    /**
     * Checks the current status of the Actor.
     * @return current status of the Actor.
     */
    public abstract boolean isActive();

    /**
     * Checks whether the current thread is the actor's current thread.
     * @return whether the current thread is the actor's current thread
     */
    public abstract boolean isActorThread();

    /**
     * Joins the actor. Waits for its termination.
     */
    public abstract void join() throws InterruptedException;

    /**
     * Joins the actor. Waits fot its termination.
     * @param milis Timeout in miliseconds
     */
    public abstract void join(long milis) throws InterruptedException;

    public Class getRemoteClass() {
        return RemoteActor.class;
    }

    public static class RemoteActor extends Actor implements RemoteSerialized {
        private final RemoteHost remoteHost;

        public RemoteActor(RemoteHost host) {
            remoteHost = host;
        }

        public Actor start() {
            throw new UnsupportedOperationException();
        }

        public Actor stop() {
            remoteHost.write(new StopActorMsg(this));
            return this;
        }

        public boolean isActive() {
            throw new UnsupportedOperationException();
        }

        public boolean isActorThread() {
            throw new UnsupportedOperationException();
        }

        public void join() {
            throw new UnsupportedOperationException();
        }

        public void join(long milis) {
            throw new UnsupportedOperationException();
        }

        public MessageStream send(Object message) {
            if (!(message instanceof ActorMessage)) {
                message = new ActorMessage<Object> (message, ReplyRegistry.threadBoundActor());
            }
            remoteHost.write(new SendTo(this, (ActorMessage) message));
            return this;
        }

        public static class StopActorMsg extends AbstractMsg {
            public final Actor actor;

            public StopActorMsg(RemoteActor remoteActor) {
                super();
                actor = remoteActor;
            }

            @Override
            public void execute(RemoteConnection conn) {
                actor.stop();
            }
        }
    }
}
