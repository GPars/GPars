package org.gparallelizer.dataflow

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import org.gparallelizer.actors.pooledActors.PooledActor
import org.gparallelizer.actors.pooledActors.PooledActors

/**
 * Created by IntelliJ IDEA.
 * User: vaclav
 * Date: Jun 4, 2009
 * Time: 1:20:29 PM
 * To change this template use File | Settings | File Templates.
 */

public class DataFlowVariable<T> {

    final AtomicReference<T> value = new AtomicReference<T>()
    final ConcurrentLinkedQueue<PooledActor> blockedReaders = new ConcurrentLinkedQueue<PooledActor>()

    private PooledActor inActor = new In(variable : this).start()  //todo escaping reference here
    
    //todo a group

    public T bitwiseNegate() {
        final def ref = value.get()
        if (ref!=null) return ref
        else {
            //todo eliminate the need for Out
            final def outActor = new Out(variable : this).start()
            blockedReaders.offer(outActor)
            return sendAndWait(outActor, new Get())
        }
    }

    private sendAndWait(PooledActor actor, def message) {
        volatile T result = null
        final def latch = new CountDownLatch(1)

        PooledActors.actor {
            actor << message
            react {
                result = it
                latch.countDown()
            }
        }.start()

        //todo blocking here, expand the pool or use Phaser in F/J
        latch.await()
        return result
    }

    public void leftShift(Object value) {
        inActor << new Set(value:value)
    }

    public void leftShift(DataFlowVariable ref) {
        inActor << new Set(value:~ref)
    }

    public void shutdown() {
        inActor.stop()
    }
}

private class DataFlowMessage{}

private class Set<T> extends DataFlowMessage {
    T value
}

private class Get extends DataFlowMessage {}

private class In extends AbstractPooledActor {

    DataFlowVariable variable

    void act() {
        loop {
            react {
                switch (it) {
                    case Set:
                        if (variable.value.compareAndSet(null, it.value)) {
                            for(reader in variable.blockedReaders) {
                                reader << new Set(value : it.value)
                            }
                            variable.blockedReaders.clear()
                        } else {
                            throw new IllegalStateException("Attempt to change data flow variable (from [${variable.value.get()}] to [${it.value}])")
                        }
                        break
                    default:throw new IllegalStateException("Cannot handle a message: $it")
                }
            }
        }
    }
}

private class Out extends AbstractPooledActor {

    DataFlowVariable variable
    private Object storedMessage

    //todo ensure correct generics
    //todo add types to messages where possible
    void act() {
        loop {
            react {
                switch (it) {
                    case Get:
                        final def ref = variable.value.get()
                        if (ref != null) {
                            it.reply(ref)
                        } else {
                            storedMessage = it
                        }
                        break
                    case Set:
                        if (storedMessage != null) storedMessage.reply(it.value)
                        break
                    default:throw new IllegalStateException("Cannot handle a message: $it")
                }
            }
        }
    }
}

private class IsolatedEventBasedThread extends AbstractPooledActor {

    Closure body

    void act() {
        //todo consider adding a loop
        body.delegate = this
        body()
    }
}

private class ReactiveEventBasedThread extends AbstractPooledActor {

    Closure body

    //todo reconsider the loop
    void act() {
        body.delegate = this
        loop {
            react {
                it.reply body(it)
            }
        }
    }
}