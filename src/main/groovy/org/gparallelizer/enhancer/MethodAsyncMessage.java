package org.gparallelizer.enhancer;

import groovy.lang.MetaClass;

import java.util.Arrays;

/**
 * A message used to invoke intercepted methods
 *
 * @author Jan Kotek, Vaclav Pech
 * Date: Apr 28, 2009
 */
final class MethodAsyncMessage extends AsyncMessage {
    private final Object object;
    private final String methodName;
    private final Object argument;
    private final Object[] arguments;

    MethodAsyncMessage(final MetaClass objectMetaClass, final Object object, final String methodName, final Object argument, final Object[] arguments) {
        super(objectMetaClass);
        this.object = object;
        this.methodName = methodName;
        this.argument = argument;
        this.arguments = Arrays.copyOf(arguments, arguments.length);
    }

    public Object getObject() {
        return object;
    }

    public String getMethodName() {
        return methodName;
    }

    public Object getArgument() {
        return argument;
    }

    public Object[] getArguments() {
        return Arrays.copyOf(arguments, arguments.length);
    }
}