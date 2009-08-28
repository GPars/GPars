package org.gparallelizer.enhancer;

import groovy.lang.MetaClass;

import java.util.Arrays;

/**
 * A message used to invoke intercepted constructors
 *
 * @author Jan Kotek, Vaclav Pech
 * Date: Apr 28, 2009
 */
final class ConstructorAsyncMessage extends AsyncMessage {
    private final Object[] arguments;

    ConstructorAsyncMessage(final MetaClass objectMetaClass, final Object[] arguments) {
        super(objectMetaClass);
        this.arguments = Arrays.copyOf(arguments, arguments.length);
    }

    public Object[] getArguments() {
        return Arrays.copyOf(arguments, arguments.length);
    }
}