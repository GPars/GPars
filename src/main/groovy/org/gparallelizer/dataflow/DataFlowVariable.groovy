package org.gparallelizer.dataflow

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import org.gparallelizer.actors.pooledActors.PooledActor
import org.gparallelizer.dataflow.DataFlowActor
import org.gparallelizer.dataflow.SetMessage
import static org.gparallelizer.dataflow.DataFlowMessage.EXIT

/**
 * Created by IntelliJ IDEA.
 * User: vaclav
 * Date: Jun 4, 2009
 * Time: 1:20:29 PM
 * To change this template use File | Settings | File Templates.
 */

public class DataFlowVariable<T> {

    final AtomicReference<T> value = new AtomicReference<T>()
    private PooledActor inActor = new In<T>(value : value).start()
    
    public T bitwiseNegate() {
        final def currentValue = value.get()
        if (currentValue!=null) return currentValue
        else {
            final def outActor = new Out<T>(inActor : inActor).start()
            return outActor.retrieveResult()
        }
    }

    public void leftShift(Object value) { inActor << new SetMessage(value) }

    public void leftShift(DataFlowVariable ref) { inActor << new SetMessage(~ref) }

    public void shutdown() { inActor << EXIT }
}

//todo ensure correct generics
//todo add types to messages where possible

private class In<T> extends DataFlowActor {

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
                            for(reader in blockedReaders) {
                                reader.reply new SetMessage<T>(it.value)
                            }
                            blockedReaders.clear()
                        } else {
                            throw new IllegalStateException("Attempt to change data flow variable (from [${value.get()}] to [${it.value}])")
                        }
                        break
                    case EXIT:
                        for(reader in blockedReaders) { reader.reply EXIT }
                        blockedReaders.clear()
                        stop()
                        break
                    default:throw new IllegalStateException("Cannot handle a message: $it")
                }
            }
        }
    }

    public void onException(Exception exception) {
        //todo handle correctly
        //todo document threads being actors
        //todo test thread lifecycle methods
        println 'Exception occured '
        exception.printStackTrace()
    }
}

private class Out<T> extends DataFlowActor {

    PooledActor inActor

    private volatile T result = null
    private final def latch = new CountDownLatch(1)

    void act() {
        inActor << new Get<T>()
        loop {
            react {
                switch (it) {
                    case SetMessage:
                        result = it.value
                        latch.countDown()
                        stop()
                        break
                    case EXIT:
                        stop()
                        break
                    default:throw new IllegalStateException("Cannot handle a message: $it")
                }
            }
        }
    }

    public void onException(Exception exception) {
        //todo handle correctly
        println 'Exception occured ' + this
        exception.printStackTrace()
    }

    //todo blocking here, expand the pool or use Phaser in F/J
    T retrieveResult() {
        latch.await()
        return result
    }
}


//private class ReactiveEventBasedThread extends DataFlowActor {
//
//    Closure body
//
//    //todo reconsider the loop
//    //todo reconsider the method
//    void act() {
//        body.delegate = this
//        loop {
//            react {
//                it.reply body(it)
//            }
//        }
//    }
//}

