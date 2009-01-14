package org.gparallelizer.enhancer

import java.util.concurrent.ExecutorService
import org.gparallelizer.enhancer.AbstractEnhancer
import org.gparallelizer.enhancer.PoolEnhancer
import org.gparallelizer.enhancer.ThreadEnhancer

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

    public static void enhanceInstance(Object instance) {
        new ThreadEnhancer().enhanceInstance(instance)
    }

    public static void enhanceInstance(Object instance, ExecutorService pool) {
        new PoolEnhancer(pool).enhanceInstance(instance)
    }

    //todo write doc
    //todo revisit
    public static void unenhanceClass(Class clazz) {
//        final def originalMethodMissing = clazz.metaClass.originalMethodMissing
//        if (originalMethodMissing) {
//            clazz.metaClass.methodMissing = originalMethodMissing
//            clazz.metaClass.originalMethodMissing = null
//        }
        clazz.metaClass.methodMissing = {String methodName, args ->
            throw new MissingMethodException(methodName, delegate.class, args)
        }
    }
}

//todo enable chaining and unenhancing
abstract private class AbstractEnhancer {
    private Closure handler = {String methodName, args ->
            if ((args.size() > 0) && (args[-1] instanceof Closure)) {
                final def originalArgs = (args.size() > 1) ? args[0..<-1] : []
                if (delegate.metaClass.respondsTo(delegate, methodName, * originalArgs)) {
                    delegate.metaClass."$methodName" = {Object[] varArgs ->
                        final def originalVarArgs = (varArgs.size() > 1) ? varArgs[0..<-1] : []
                        schedule {
                            def result
                            try {
                                result = delegate."$methodName"(* originalVarArgs)
                            } catch (Throwable e) {
                                result = e
                            }
                            varArgs[-1].call(result)
                        }
                    }
                    delegate."$methodName"(args)
                    return
                }
            }
//            if (clazz.metaClass.originalMethodMissing) ((ExpandoMetaProperty)clazz.metaClass.originalMethodMissing).getProperty().call(methodName, args)
//            else
                throw new MissingMethodException(methodName, delegate.class, args)
        }

    public final void enhanceClass(Class clazz) {

//        clazz.metaClass.originalMethodMissing = clazz.metaClass.methodMissing

        clazz.metaClass.methodMissing = handler
    }

    public final void enhanceInstance(Object instance) {

//        clazz.metaClass.originalMethodMissing = clazz.metaClass.methodMissing

        instance.metaClass.methodMissing = handler
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
