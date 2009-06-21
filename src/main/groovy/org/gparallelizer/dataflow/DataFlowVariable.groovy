package org.gparallelizer.dataflow

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import org.gparallelizer.actors.pooledActors.PooledActor
import org.gparallelizer.dataflow.DataFlowActor
import org.gparallelizer.dataflow.SetMessage
import static org.gparallelizer.dataflow.DataFlowMessage.EXIT
import org.gparallelizer.actors.ActorMessage

/**
 * Represents a thread-safe single-assignment, multi-read variable.
 * Each instance of DataFlowVariable can be read repetadly any time using the '~' operator and assigned once
 * in its lifetime using the '<<' operator. Reads preceding assignment will be blocked until the value
 * is assigned.
 *
 * Experimental implementation only. Implemented using pooled actors.
 * @author Vaclav Pech
 * Date: Jun 4, 2009
 */
public final class ActorBasedDataFlowVariable<T> {

    /**
     * Holds the value associated with the DataFlowVariable
     */
    final AtomicReference<T> value = new AtomicReference<T>()

    /**
     * Internal actor managing internal state on the DataFlowVariable
     */
    private final PooledActor inActor = new In<T>(value: value).start()

    /**
     * Holds the current state of the variable
     */
    private final AtomicReference<DataFlowState> state = new AtomicReference<DataFlowState>(DataFlowState.UNASSIGNED)

    /**
     * Reads the value of the variable. Blocks, if the value has not been assigned yet.
     */
    public T bitwiseNegate() {
        final def currentValue = value.get()
        if (currentValue != null) return currentValue
        else {
            final def outActor = new Out<T>(inActor: inActor, value : value).start()
            return outActor.retrieveResult()
        }
    }

    /**
     * Assigns a value to the variable. Can only be invoked once on each instance of DataFlowVariable
     */
    public void leftShift(Object value) {
        if (!state.compareAndSet(DataFlowState.UNASSIGNED, DataFlowState.ASSIGNED)) throw new IllegalStateException("The ActorBasedDataFlowVariable cannot be assigned a value.")
        inActor << new SetMessage(value)
    }

    /**
     * Assigns a value from one DataFlowVariable instance to this variable.
     * Can only be invoked once on each instance of DataFlowVariable
     */
    public void leftShift(ActorBasedDataFlowVariable ref) {
        if (!state.compareAndSet(DataFlowState.UNASSIGNED, DataFlowState.ASSIGNED)) throw new IllegalStateException("The ActorBasedDataFlowVariable cannot be assigned a value.")
        inActor << new SetMessage(~ref)
    }

    /**
     * Stops the variable by sending the EXIT message to the internal actor and all registered read actors.
     */
    public void shutdown() {
        if (DataFlowState.STOPPED == state.getAndSet(DataFlowState.STOPPED)) throw new IllegalStateException("The ActorBasedDataFlowVariable has been stopped already.")
        if (inActor.isActive()) inActor << EXIT
    }
}


//todo don't In stop after SetMessage - make sure the memory is cleared
//todo remove shutdown()
//todo use sendAndWait(), remove Out
//todo ensure correct generics
//todo add types to messages where possible
//todo why not use a simpler non-actor implementation
//todo rename and comment samples

/**
 * An actor guarding internal state of the DataFlow Variable and communicates the state to instances of the Out actor class.
 */
private final class In<T> extends DataFlowActor {

    AtomicReference<T> value

    final ConcurrentLinkedQueue blockedReaders = new ConcurrentLinkedQueue()

    void act() {
        loop {
            react {
                switch (it) {
                    case Get:
                        if (value.get() == null) {
                            blockedReaders.add(it)
                        } else {
                            it.reply new SetMessage<T>(value.get())
                        }
                        break
                    case SetMessage:
                        if (value.compareAndSet(null, it.value)) {
                            for (reader in blockedReaders) {
                                reader.reply new SetMessage<T>(it.value)
                            }
                            blockedReaders.clear()
                            this << EXIT
                        } else {
                            assert false  //prevented by the state
                        }
                        break
                    case EXIT:
                        for (reader in blockedReaders) { reader.reply EXIT }
                        blockedReaders.clear()
                        stop()
                        break
                    default: assert false  //prevented by the architecture
                }
            }
        }
    }

    /**
     * Inform all unprocessed Get messages from Out actors that this actor is exiting.
     * If there's a value set on the DataFlowVariable, respond with SetMessage containing the value,
     * otherwise respond with EXIT.
     */
    private void afterStop(List<ActorMessage> messages) {
        for(ActorMessage message in messages) {
            switch (message.payLoad) {
                case Get:
                    if (value.get()!=null) {
                        message.sender << new SetMessage<T>(value.get())  //todo InterruptedException thrown here occasionally
                    } else {
                        message.sender << EXIT  //todo InterruptedException thrown here occasionally
                    }
                    break
                default:
                    break
            }
        }
    }

    private void onException(Exception exception) {
        exception.printStackTrace(System.err)
    }
}

/**
 * Represents a waiting read request. Sends a Get message to the In actor of the particular data flow variable
 * and waits for a Set message once the value is available.
 */
private final class Out<T> extends DataFlowActor {

    PooledActor inActor

    AtomicReference<T> value

    private volatile T result = null
    private volatile Exception reason = null
    //todo replace with ActorBarrier
    private final def latch = new CountDownLatch(1)

    void act() {
        try {
            inActor << new Get<T>()
        } catch (IllegalStateException e) {
            if (value.get()!=null) {
                result = value.get()
                latch.countDown()
            } else {
                throw e
            }
        }
        
        react {
            try {
                switch (it) {
                    case SetMessage:
                        result = it.value
                        break
                    case EXIT:
                        reason = new IllegalStateException("ActorBasedDataFlowVariable stopped before assignment")
                        break
                    default:
                        reason = new IllegalStateException("Cannot handle a message: $it")
                }
            } finally {
                latch.countDown()
            }
        }
    }

    private void onException(Exception exception) {
        reason = new IllegalStateException("The ActorBasedDataFlowVariable cannot be retrieved and has not been assigned a value.", exception)
        latch.countDown()
    }

    //todo blocking here, expand the pool or use Phaser in F/J
    /**
     * Retrieves the value once received by the actor.
     * Throws an exception if an exception occured while waiting for the message.
     */
    T retrieveResult() {
        latch.await()
        if (reason) throw reason
        else return result
    }
}
