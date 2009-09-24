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

import groovy.time.Duration;
import org.gparallelizer.MessageStream;
import org.gparallelizer.ReceivingMessageStream;
import org.gparallelizer.dataflow.DataFlowExpression;
import org.gparallelizer.dataflow.DataFlowVariable;
import org.gparallelizer.remote.RemoteConnection;
import org.gparallelizer.remote.RemoteHost;
import org.gparallelizer.serial.*;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

/**
 * Actors are active objects, which either have their own thread processing repeatedly messages submitted to them
 * or they borrow a thread from a thread pool. Actors implementing the ThreadedActor interface have their own thread,
 * whereas actors implementing PooledActor interface delegate processing to threads from a thread pool.
 * The Actor interface provides means to send messages to the actor, start and stop the background thread as well as
 * check its status.
 *
 * @author Vaclav Pech, Alex Tkachman
 */
public abstract class Actor extends ReceivingMessageStream {

    /**
     * Maps each thread to the actor it currently processes.
     * Used in the send() method to remember the sender of each message for potential replies
     */
    private static final ThreadLocal<Actor> currentActorPerThread = new ThreadLocal<Actor>();

    private final DataFlowExpression joinLatch;

    protected Actor() {
        this(new DataFlowVariable());
    }

    /**
     * Constructor to be used by deserialization
     *
     * @param joinLatch
     */
    protected Actor(DataFlowExpression joinLatch) {
        this.joinLatch = joinLatch;
    }

    /**
     * Starts the Actor. No messages can be send or received before an Actor is started.
     *
     * @return same actor
     */
    public abstract Actor start();

    /**
     * Stops the Actor. Unprocessed messages will be passed to the afterStop method, if exists.
     * Has no effect if the Actor is not started.
     *
     * @return same actor
     */
    public abstract Actor stop();

    /**
     * Checks the current status of the Actor.
     *
     * @return current status of the Actor.
     */
    public abstract boolean isActive();

    /**
     * Checks whether the current thread is the actor's current thread.
     *
     * @return whether the current thread is the actor's current thread
     */
    public abstract boolean isActorThread();

    /**
     * Joins the actor. Waits for its termination.
     *
     * @throws InterruptedException when interrupted while waiting
     */
    public final void join() throws InterruptedException {
        joinLatch.getVal();
    }

    /**
     * Notify listener when finished
     *
     * @param listener listener to notify
     * @throws InterruptedException if interrupted while waiting
     */
    public final void join(MessageStream listener) throws InterruptedException {
        joinLatch.getValAsync(listener);
    }

    /**
     * Joins the actor. Waits for its termination.
     *
     * @param timeout timeout
     * @param unit    units of timeout
     * @throws InterruptedException if interrupted while waiting
     */
    public final void join(final long timeout, TimeUnit unit) throws InterruptedException {
        if (timeout > 0)
            joinLatch.getVal(timeout, unit);
        else
            joinLatch.getVal();
    }

    /**
     * Joins the actor. Waits for its termination.
     *
     * @param duration timeout to wait
     * @throws InterruptedException if interrupted while waiting
     */
    public final void join(Duration duration) throws InterruptedException {
        join(duration.toMilliseconds(), TimeUnit.MILLISECONDS);
    }

    /**
     * Join-point for this actor
     *
     * @return
     */
    public DataFlowExpression getJoinLatch() {
        return joinLatch;
    }

    /**
     * Registers the actor with the current thread
     *
     * @param currentActor The actor to register
     */
    protected static void registerCurrentActorWithThread(final Actor currentActor) {
        currentActorPerThread.set(currentActor);
    }

    /**
     * Deregisters the actor registered from the thread
     */
    protected static void deregisterCurrentActorWithThread() {
        currentActorPerThread.set(null);
    }

    /**
     * Retrieves the actor registered with the current thread
     *
     * @return The associated actor
     */
    public static Actor threadBoundActor() {
        return currentActorPerThread.get();
    }

    @Override
    protected RemoteHandle createRemoteHandle(SerialHandle handle, SerialContext host) {
        return new MyRemoteHandle(handle, host, joinLatch);
    }

    public static class MyRemoteHandle extends DefaultRemoteHandle {
        private final DataFlowExpression joinLatch;

        public MyRemoteHandle(SerialHandle handle, SerialContext host, DataFlowExpression joinLatch) {
            super(handle.getSerialId(), host.getHostId(), RemoteActor.class);
            this.joinLatch = joinLatch;
        }

        @Override
        protected WithSerialId createObject(SerialContext context) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
            return new RemoteActor(context, joinLatch);
        }
    }

    public static class RemoteActor extends Actor implements RemoteSerialized {
        private final RemoteHost remoteHost;

        public RemoteActor(SerialContext host, DataFlowExpression jointLatch) {
            super(jointLatch);
            remoteHost = (RemoteHost) host;
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

        public MessageStream send(Object message) {
            if (!(message instanceof ActorMessage)) {
                message = new ActorMessage<Object>(message, threadBoundActor());
            }
            remoteHost.write(new SendTo(this, (ActorMessage) message));
            return this;
        }

        protected Object receiveImpl() throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        protected Object receiveImpl(long timeout, TimeUnit units) throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        public static class StopActorMsg extends SerialMsg {
            private final Actor actor;

            public StopActorMsg(RemoteActor remoteActor) {
                actor = remoteActor;
            }

            @Override
            public void execute(RemoteConnection conn) {
                actor.stop();
            }
        }
    }
}
