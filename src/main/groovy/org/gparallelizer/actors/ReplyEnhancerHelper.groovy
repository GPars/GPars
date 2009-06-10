package org.gparallelizer.actors;

/**
 * Enables actors and messages to send replies.
 *
 * @author Vaclav Pech
 * Date: Apr 15, 2009
 */
public abstract class ReplyEnhancerHelper {

    /**
     * Enhances the replier's metaClass with reply() and replyIfExists() methods to send messages to the sender
     */
    static def enhanceObject(final def replier, final List<Actor> senders) {
        //call to getMetaClass() is required, since maps don't handle metaClass property access correctly
        replier.getMetaClass().reply = {msg ->
            if (!senders.isEmpty()) {
                for(sender in senders) {
                    if (sender!=null) sender.send msg
                    else throw new IllegalArgumentException("Cannot send a reply message ${msg} to a null recipient.")
                }
            } else {
                throw new IllegalArgumentException("Cannot send replies. The list of recipients is empty.")
            }
        }

        replier.getMetaClass().replyIfExists = {msg ->
            try {
                for(sender in senders) sender?.send msg
            } catch (IllegalStateException ignore) { }
        }
    }
}