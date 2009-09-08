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

/**
 * A shared helper class to invoke the intercepted method and process the return value
 * used by actors associated with ActorMetaClass.
 *
 * @author Vaclav Pech
 * Date: Apr 28, 2009
 */
@Singleton
public final class EnhancerHelper {

    /**
     * Invokes the original method based on the data in the message and sets the return value back to the message
     */
    def processMessage(AsyncMessage message) {
        try {
            Object result = invokeOriginalMethod(message)
            if (result == null) message.setReturnValue AsyncMessage.NULL
            else message.setReturnValue result
        } catch (Throwable e) {
            message.setReturnValue e;
        }
    }

    private Object invokeOriginalMethod(AsyncMessage message) {
        switch (message) {
            case ConstructorAsyncMessage:
                return message.objectMetaClass.invokeConstructor(message.arguments)

            case MethodAsyncMessage:
                if (message.argument != null)
                    return message.objectMetaClass.invokeMethod(message.object, message.methodName, message.argument);
                else
                    return message.objectMetaClass.invokeMethod(message.object, message.methodName, message.arguments);
                break

            default:
                throw new IllegalArgumentException("Cannot handle the message: $message")
        }
    }
}
