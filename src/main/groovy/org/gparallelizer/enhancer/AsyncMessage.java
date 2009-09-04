package org.gparallelizer.enhancer;

import groovy.lang.MetaClass;
import org.gparallelizer.dataflow.DataFlowVariable;

/**
 * A message used to invoke intercepted constructors or methods.
 * It holds a reference back to the ActorMetaClass to allow for invokation of the intercepted methods.
 * It also keeps a write-once volatile returnValue property to pass result back from the actor to the caller.
 *
 * @author Jan Kotek, Vaclav Pech, Alex Tkachman
 * Date: Apr 28, 2009
 */
@SuppressWarnings({"MethodReturnOfConcreteClass", "InstanceVariableOfConcreteClass"})
class AsyncMessage {
    private final MetaClass objectMetaClass;
    private final DataFlowVariable<Object> returnValue=new DataFlowVariable<Object>();

    protected static final Object NULL = new Object();

    AsyncMessage(final MetaClass objectMetaClass) {
        this.objectMetaClass = objectMetaClass;
    }

    final MetaClass getObjectMetaClass() { return objectMetaClass; }

    final Object getReturnValue() throws InterruptedException { return returnValue.getVal(); }

    final void setReturnValue(final Object returnValue) {
        this.returnValue.bind(returnValue);
    }

    final DataFlowVariable getResultHolder() { return returnValue; }

    /**
     * Utility method to copy array
     *
     * @param array array to copy
     * @return clone
     */
    protected static Object [] copyOf (Object [] array) {
        if (array == null)
            return null;

        final Object[] res = new Object[array.length];
        System.arraycopy(array, 0, res, 0, array.length);
        return res;
    }
}
