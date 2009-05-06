package org.gparallelizer.enhancer;

import groovy.lang.DelegatingMetaClass;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.gparallelizer.actors.Actor;

/**
 * A meta class that intercepts all method calls to the instances, which it is attached to,
 * suspends the calling thread and processes the called method in the associated actor.
 * Each ActorMetaClass instance has an actor associated. Based on the constructor used tha actor can be
 * either thread-bound or event-driven (pooled).
 *
 * @author Jan Kotek, Vaclav Pech
 * Date: Apr 28, 2009
 */
public class ActorMetaClass extends DelegatingMetaClass {

    /**
     * The background actor for processing all incoming method calls
     */
    private final Actor actor;

    /**
     * Creates a new instance with an event-driven actor associated
     * @param clazz The class to intercept
     */
    public ActorMetaClass(final Class clazz) {
        this(clazz, false);
    }

    /**
     * Creates a new instance with an actor associated
     * @param clazz The class to intercept
     * @param pooledActor True if an event-driven (pooled) actor should be used, false for a thread-bound actor
     */
    public ActorMetaClass(final Class clazz, final boolean pooledActor) {
        super(clazz);
        if (pooledActor) actor = new EnhancerPooledActor();
        else actor = new EnhancerActor();
        actor.start();
        //todo hide constructors
        //TODO shutdown of thread-bound actor
        //todo test
        //todo enable actor groups - a dedicated group
    }

    /**
     * Enhances the supplied class with the ActorMetaClass
     * @param clazz The class to intercept
     */
    public static void intercept(final Class clazz) {
        ActorMetaClass.intercept(clazz, false);
    }

    /**
     * Enhances the supplied class with the ActorMetaClass
     * @param clazz The class to intercept
     * @param pooledActor True if an event-driven (pooled) actor should be used, false for a thread-bound actor
     */
    public static void intercept(final Class clazz, final boolean pooledActor) {
        InvokerHelper.metaRegistry.setMetaClass(clazz, new ActorMetaClass(clazz, pooledActor));
    }

    /**
     * Intercepts constructor calls
     * @param arguments The original arguments
     * @return The original return value
     */
    @Override
    public Object invokeConstructor(final Object[] arguments) {
        if (actor.isActorThread()) {
            return super.invokeConstructor(arguments);
        } else {
            return performAsyncMethodCall(new ConstructorAsyncMessage(this, arguments));
        }
    }

    /**
     * Intercepts method calls
     * @param object The object the method is being invoked on
     * @param methodName The name of intercepted method
     * @param argument The original argument
     * @return The original return value returned from the intercepted method
     */
    @Override
    public Object invokeMethod(final Object object, final String methodName, final Object argument) {
        if (actor.isActorThread()) {
            return super.invokeMethod(object, methodName, argument);
        } else {
            return performAsyncMethodCall(new MethodAsyncMessage(this, object, methodName, argument, null));
        }
    }

    /**
     * Intercepts method calls
     * @param object The object the method is being invoked on
     * @param methodName The name of intercepted method
     * @param arguments The original arguments
     * @return The original return value returned from the intercepted method
     */
    @Override
    public Object invokeMethod(final Object object, final String methodName, final Object[] arguments) {
        if (actor.isActorThread()) {
            return super.invokeMethod(object, methodName, arguments);
        } else {
            return performAsyncMethodCall(new MethodAsyncMessage(this, object, methodName, null, arguments));
        }
    }

    /**
     * Sends the message to the associated actor, waits for the reply and processes it
     * @param msg The message to pass to the actor
     * @return The original return value returned from the intercepted method
     */
    private Object performAsyncMethodCall(final AsyncMessage msg) {
        try {
            actor.send(msg);
            msg.await();
            final Object value = msg.getReturnValue();
            if (value instanceof Throwable) {
                throw new RuntimeException((Throwable) value);
            } else if (value == AsyncMessage.NULL) {
                return null;
            } else {
                return value;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}