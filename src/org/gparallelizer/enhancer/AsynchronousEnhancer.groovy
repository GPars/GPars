package org.gparallelizer.enhancer

import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService

/**
 * Experimental state
 *
 * @author Vaclav Pech
 * Date: Jan 9, 2009
 */
public class AsynchronousEnhancer {

    public static void enhanceClass(Class clazz) {
        new ThreadEnhancer().enhanceClass(clazz)
    }

    public static void enhanceClass(Class clazz, ExecutorService pool) {
        new PoolEnhancer(pool).enhanceClass(clazz)
    }

    //todo write doc
    //todo revisit
    public static void unenhanceClass(Class clazz) {
        final def originalInvokeMethod = clazz.metaClass.originalInvokeMethod
        if (originalInvokeMethod) {
            clazz.metaClass.invokeMethod = {String methodName, Object[] args ->
                originalInvokeMethod.invokeMethod(methodName, args)
            }
            clazz.metaClass.originalInvokeMethod = null
        }
    }
}

//todo enhance property retrieval - test
abstract private class AbstractEnhancer {
    public final void enhanceClass(Class clazz) {
        def originalInvokeMethod = clazz.metaClass.invokeMethod
        clazz.metaClass.originalInvokeMethod = originalInvokeMethod

        clazz.metaClass.invokeMethod = {String methodName, Object[] args ->
            def metaMethod = String.metaClass.getMetaMethod(methodName, args)
            if ((!metaMethod) && (args.size() > 0) && (args[-1] instanceof Closure)) {
                def originalArgs = args.size() > 1 ? args[0..-2] : []
                final def method = delegate.class.metaClass.getMetaMethod(methodName, * originalArgs)
                if (method) {
                    schedule {
                        def result
                        try {
                            result = delegate."$methodName"(* originalArgs)
                        } catch (Exception e) {
                            result = e
                        }
                        args[-1].call(result)
                    }
                    return
                } else {
                    if ((args.size() == 1) && (args[0] instanceof Closure)) {
                        final def property = delegate.class.metaClass.getMetaProperty(methodName)
                        if (property) {
                            schedule {
                                def result
                                try {
                                    result = delegate."$methodName"
                                } catch (Exception e) {
                                    result = e
                                }
                                args[0].call(result)
                            }
                            return
                        }
                    }
                }
            }
            originalInvokeMethod.invokeMethod(methodName, args)
        }
    }

    abstract protected void schedule(Closure task)
}

private class ThreadEnhancer extends AbstractEnhancer {

    protected void schedule(Closure task) {
        Thread.start(task)
    }
}

private class PoolEnhancer extends AbstractEnhancer {

    ExecutorService pool

    def PoolEnhancer(final ExecutorService pool) {
        this.pool = pool;
    }

    protected void schedule(Closure task) {
        pool.submit(task)
    }
}
