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
