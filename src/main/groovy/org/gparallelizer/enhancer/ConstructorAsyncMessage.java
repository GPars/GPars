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
