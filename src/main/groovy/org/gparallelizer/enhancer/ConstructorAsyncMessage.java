package org.gparallelizer.enhancer;

import groovy.lang.MetaClass;

import java.util.Arrays;

/**
 * A message used to invoke intercepted constructors
 *
 * @author Jan Kotek, Vaclav Pech, Alex Tkachman
 * Date: Apr 28, 2009
 */
final class ConstructorAsyncMessage extends AsyncMessage {
    private final Object[] arguments;

    ConstructorAsyncMessage(final MetaClass objectMetaClass, final Object[] arguments) {
        super(objectMetaClass);
        this.arguments = copyOf(arguments);
    }

    public Object[] getArguments() {
        return copyOf(arguments);
    }
}