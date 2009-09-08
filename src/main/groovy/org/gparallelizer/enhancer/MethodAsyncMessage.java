//  GParallelizer
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License. 

package org.gparallelizer.enhancer;

import groovy.lang.MetaClass;

import java.util.Arrays;

/**
 * A message used to invoke intercepted methods
 *
 * @author Jan Kotek, Vaclav Pech, Alex Tkachman
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
        this.arguments = copyOf(arguments);
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
        return copyOf(arguments);
    }
}
