package org.gparallelizer.enhancer;

import groovy.lang.MetaClass;

import java.util.concurrent.CountDownLatch;

/**
 * A message used to invoke intercepted constructors or methods.
 * It holds a reference back to the ActorMetaClass to allow for invokation of the intercepted methods.
 * It also keeps a write-once volatile returnValue property to pass result back from the actor to the caller.
 *
 * @author Jan Kotek, Vaclav Pech
 * Date: Apr 28, 2009
 */
class AsyncMessage {
    private final MetaClass objectMetaClass;
    private volatile Object returnValue=null;

    private final CountDownLatch latch = new CountDownLatch(1);

    protected static final Object NULL = new Object();

    AsyncMessage(final MetaClass objectMetaClass) {
        this.objectMetaClass = objectMetaClass;
    }

    public final void await() throws InterruptedException {
        latch.await();
    }

    public MetaClass getObjectMetaClass() {
        return objectMetaClass;
    }

    public final Object getReturnValue() {
        return returnValue;
    }

    public final void setReturnValue(final Object returnValue) {
        if (this.returnValue != null)
            throw new IllegalStateException("Cannot set the return value on a amessage twice.");
        this.returnValue = returnValue;
        latch.countDown();
    }
}
