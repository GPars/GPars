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

import org.codehaus.groovy.runtime.InvokerHelper;

/**
 * Created by IntelliJ IDEA.
 * User: vaclav
 * Date: Aug 3, 2009
 * Time: 2:27:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class NonBlockingActorMetaClass extends ActorMetaClass {

    public static void enhanceClass(final Class clazz) {
        InvokerHelper.metaRegistry.setMetaClass(clazz, new NonBlockingActorMetaClass(clazz));
    }

    public NonBlockingActorMetaClass(final Class clazz) {
        super(clazz);
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
            return performAsyncMethodCallWithoutWait(new MethodAsyncMessage(this, object, methodName, argument, null));
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
            return performAsyncMethodCallWithoutWait(new MethodAsyncMessage(this, object, methodName, null, arguments));
        }
    }

    private Object performAsyncMethodCallWithoutWait(final AsyncMessage msg) {
        actor.send(msg);
        return msg.getResultHolder();
    }
}
