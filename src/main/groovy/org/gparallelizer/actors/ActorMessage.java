package org.gparallelizer.actors;

/**
 * An internal representation of received messages holding both the original message plus the sender actor reference.
 * This class is not intented to be use directly by users.
 *
 * @author Vaclav Pech, Alex Tkachman
 * Date: Feb 27, 2009
 */
@SuppressWarnings({"MethodReturnOfConcreteClass"})
public final class ActorMessage<T> {
    final private T payLoad;
    final private Actor sender;

    /**
     * Creates a new instance
     * @param payLoad The original message
     * @param sender The sending actor, null, if the message was not sent by an actor
     */
    private ActorMessage(final T payLoad, final Actor sender) {
        this.payLoad = payLoad;
        this.sender = sender;
    }

    public T getPayLoad() {
        return payLoad;
    }

    public Actor getSender() {
        return sender;
    }

    /**
     * Factory method to create instances of ActorMessage with given payload.
     * The sender of the ActorMessage is retrieved from the ReplyRegistry.
     * @param payLoad The original message
     * @return The newly created message
     */
    public static <T> ActorMessage build(final T payLoad) {
        return new ActorMessage<T>(payLoad, ReplyRegistry.threadBoundActor());
    }

    @Override public String toString() {
        return "Message from $sender: $payLoad";
    }
}
