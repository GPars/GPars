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
package groovyx.gpars.actor;

import groovy.time.BaseDuration;
import groovyx.gpars.MessageStream;
import groovyx.gpars.ReceivingMessageStream;
import groovyx.gpars.dataflow.DataFlowExpression;
import groovyx.gpars.dataflow.DataFlowVariable;
import groovyx.gpars.remote.RemoteConnection;
import groovyx.gpars.remote.RemoteHost;
import groovyx.gpars.serial.DefaultRemoteHandle;
import groovyx.gpars.serial.RemoteHandle;
import groovyx.gpars.serial.RemoteSerialized;
import groovyx.gpars.serial.SerialContext;
import groovyx.gpars.serial.SerialHandle;
import groovyx.gpars.serial.SerialMsg;
import groovyx.gpars.serial.WithSerialId;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

/**
 * Actors are active objects, which borrow a thread from a thread pool.
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
     * @param joinLatch The instance of DataFlowExpression to use for join operation
     */
    protected Actor(final DataFlowExpression joinLatch) {
        this.joinLatch = joinLatch;
    }

    /**
     * Starts the Actor. No messages can be sent or received before an Actor is started.
     *
     * @return same actor
     */
    public abstract Actor start();

    /**
     * Send message to stop to the Actor. All messages already in queue will be processed first.
     * No new messages will be accepted since that point.
     * Has no effect if the Actor is not started.
     *
     * @return same actor
     */
    public abstract Actor stop();

    /**
     * Terminates the Actor. Unprocessed messages will be passed to the afterStop method, if exists.
     * No new messages will be accepted since that point.
     * Has no effect if the Actor is not started.
     *
     * @return same actor
     */
    public abstract Actor terminate();

    /**
     * Checks the current status of the Actor.
     *
     * @return current status of the Actor.
     */
    public abstract boolean isActive();

    /**
     * Checks whether the current thread is the actor's worker thread.
     *
     * @return whether the current thread is the actor's worker thread
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
    public final void join(final MessageStream listener) throws InterruptedException {
        joinLatch.getValAsync(listener);
    }

    /**
     * Joins the actor. Waits for its termination.
     *
     * @param timeout timeout
     * @param unit    units of timeout
     * @throws InterruptedException if interrupted while waiting
     */
    public final void join(final long timeout, final TimeUnit unit) throws InterruptedException {
        if (timeout > 0) {
            joinLatch.getVal(timeout, unit);
        } else {
            joinLatch.getVal();
        }
    }

    /**
     * Joins the actor. Waits for its termination.
     *
     * @param duration timeout to wait
     * @throws InterruptedException if interrupted while waiting
     */
    public final void join(final BaseDuration duration) throws InterruptedException {
        join(duration.toMilliseconds(), TimeUnit.MILLISECONDS);
    }

    /**
     * Join-point for this actor
     *
     * @return The DataFlowExpression instance, which is used to join this actor
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
    protected RemoteHandle createRemoteHandle(final SerialHandle handle, final SerialContext host) {
        return new MyRemoteHandle(handle, host, joinLatch);
    }

    public static class MyRemoteHandle extends DefaultRemoteHandle {
        private final DataFlowExpression joinLatch;

        public MyRemoteHandle(final SerialHandle handle, final SerialContext host, final DataFlowExpression joinLatch) {
            super(handle.getSerialId(), host.getHostId(), RemoteActor.class);
            this.joinLatch = joinLatch;
        }

        @Override
        protected WithSerialId createObject(final SerialContext context) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
            return new RemoteActor(context, joinLatch);
        }
    }

    public static class RemoteActor extends Actor implements RemoteSerialized {
        private final RemoteHost remoteHost;

        public RemoteActor(final SerialContext host, final DataFlowExpression jointLatch) {
            super(jointLatch);
            remoteHost = (RemoteHost) host;
        }

        @Override public Actor start() {
            throw new UnsupportedOperationException();
        }

        @Override public Actor stop() {
            remoteHost.write(new StopActorMsg(this));
            return this;
        }

        @Override public Actor terminate() {
            remoteHost.write(new TerminateActorMsg(this));
            return this;
        }

        @Override public boolean isActive() {
            throw new UnsupportedOperationException();
        }

        @Override public boolean isActorThread() {
            return false;
        }

        @SuppressWarnings({"AssignmentToMethodParameter"}) @Override
        public MessageStream send(Object message) {
            if (!(message instanceof ActorMessage)) {
                message = new ActorMessage<Object>(message, threadBoundActor());
            }
            remoteHost.write(new SendTo(this, (ActorMessage) message));
            return this;
        }

        @Override protected Object receiveImpl() throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override protected Object receiveImpl(final long timeout, final TimeUnit units) throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        public static class StopActorMsg extends SerialMsg {
            private final Actor actor;

            public StopActorMsg(final RemoteActor remoteActor) {
                actor = remoteActor;
            }

            @Override
            public void execute(final RemoteConnection conn) {
                actor.stop();
            }
        }

        public static class TerminateActorMsg extends SerialMsg {
            private final Actor actor;

            public TerminateActorMsg(final RemoteActor remoteActor) {
                actor = remoteActor;
            }

            @Override
            public void execute(final RemoteConnection conn) {
                actor.terminate();
            }
        }
    }
}
