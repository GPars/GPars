package org.gparallelizer.actors.pooledActors

import groovy.time.Duration
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.gparallelizer.actors.Actor
import org.gparallelizer.actors.pooledActors.ActorAction
import org.gparallelizer.actors.pooledActors.PooledActor
import static org.gparallelizer.actors.pooledActors.ActorAction.actorAction
import static org.gparallelizer.actors.pooledActors.ActorException.*
import org.gparallelizer.actors.ActorMessage
import org.gparallelizer.actors.ReplyRegistry
import org.gparallelizer.actors.ReplyEnhancer

/**
 * AbstractPooledActor provides the default PooledActor implementation. It represents a standalone active object (actor),
 * which reacts asynchronously to messages sent to it from outside through the send() method.
 * Each PooledActor has its own message queue and a thread pool shared with other PooledActors.
 * The work performed by a PooledActor is divided into chunks, which are sequentially submitted as independent tasks
 * to the thread pool for processing.
 * Whenever a PooledActor looks for a new message through the react() method, the actor gets detached
 * from the thread, making the thread available for other actors. Thanks to the ability to dynamically attach and detach
 * threads to actors, PooledActors can scale far beyond the limits of the underlying platform on number of cuncurrently
 * available threads.
 * The loop() method allows repeatedly invoke a closure and yet perform each of the iterations sequentially
 * in different thread from the thread pool.
 * To suport continuations correctly the react() and loop() methods never return.
 * <pre>
 * import static org.gparallelizer.actors.pooledActors.PooledActors.*
 *
 * def actor = actor {*     loop {*         react {message ->
 *             println message
 *}*         //this line will never be reached
 *}*     //this line will never be reached
 *}.start()
 *
 * actor.send 'Hi!'
 * </pre>
 * This requires the code to be structured accordingly.
 *
 * <pre>
 * def adder = actor {*     loop {*         react {a ->
 *             react {b ->
 *                 println a+b
 *                 replyIfExists a+b  //sends reply, if b was sent by a PooledActor
 *}*}*         //this line will never be reached
 *}*     //this line will never be reached
 *}.start()
 * </pre>
 * The closures passed to the react() method can call reply() or replyIfExists(), which will send a message back to
 * the originator of the currently processed message. The replyIfExists() method unlike the reply() method will not fail
 * if the original message wasn't sent by an actor nor if the original sender actor is no longer running.
 * The react() method accepts timout specified using the TimeCategory DSL.
 * <pre>
 * react(10.MINUTES) {*     println 'Received message: ' + it
 *}* </pre>
 * If not message arrives within the given timeout, the onTimeout() lifecycle handler is invoked, if exists,
 * and the actor terminates.
 * Each PooledActor has at any point in time at most one active instance of ActorAction associated, which abstracts
 * the current chunk of actor's work to perform. Once a thread is assigned to the ActorAction, it moves the actor forward
 * till loop() or react() is called. These methods schedule another ActorAction for processing and throw dedicated exception
 * to terminate the current ActorAction. 
 *
 * @author Vaclav Pech
 * Date: Feb 7, 2009
 */
abstract public class AbstractPooledActor implements PooledActor {

    /**
     * Queue for the messages
     */
    private final BlockingQueue messageQueue = new LinkedBlockingQueue();

    /**
     * Code for the next action
     */
    private final AtomicReference<Closure> codeReference = new AtomicReference<Closure>(null)

    /**
     * Buffer to hold messages required by the scheduled next continuation. Null when no continuation scheduled.
     */
    private final MessageHolder bufferedMessages = null

    /**
     * Indicates whether the actor should terminate
     */
    private final AtomicBoolean stopFlag = new AtomicBoolean(true)

    /**
     * Code for the loop, if any
     */
    private final AtomicReference<Closure> loopCode = new AtomicReference<Closure>(null)

    /**
     * The current active action (continuation) associated with the actor. An action must not use Actor's state
     * after it schedules a new action, only throw CONTINUE.
     */
    final AtomicReference<ActorAction> currentAction = new AtomicReference<ActorAction>(null)

    /**
     * The current timeout task, which will send a TIMEOUT message to the actor if not cancelled by someone
     * calling the send() method within the timeout specified for the currently blocked react() method.
     */
    private AtomicReference<TimerTask> timerTask = new AtomicReference<TimerTask>(null)

    /**
     * Internal lock to synchronize access of external threads calling send() or stop() with the current active actor action
     */
    private final Object lock = new Object()

    /**
     * Timer holding timeouts for react methods
     */
    private static final Timer timer = new Timer(true)

    /**
     * The actor group to which the actor belongs
     */
    volatile PooledActorGroup actorGroup = PooledActors.defaultPooledActorGroup

    /**
     * Indicates whether the actor's group can be changed. It is typically not changeable after actor starts.
     */
    private volatile boolean groupMembershipChangeable = true

    /**
     * Sets the actor's group.
     * It can only be invoked before the actor is started.
     */
    public final void setActorGroup(PooledActorGroup group) {
        if (!groupMembershipChangeable) throw new IllegalStateException("Cannot set actor's group on a started actor.")
        if (!group) throw new IllegalArgumentException("Cannot set actor's group to null.")
        actorGroup = group
    }

    /**
     * Starts the Actor. No messages can be send or received before an Actor is started.
     */
    public final AbstractPooledActor start() {
        groupMembershipChangeable = false
        if (!stopFlag.getAndSet(false)) throw new IllegalStateException("Actor has alredy been started.")
        actorAction(this) {
            if (delegate.respondsTo('afterStart')) delegate.afterStart()
            act()
        }
        return this
    }

    /**
     * Sets the stopFlag
     * @return The previous value of the stopFlag
     */
    final boolean indicateStop() {
        cancelCurrentTimeoutTimer('')
        return stopFlag.getAndSet(true)
    }

    /**
     * Stops the Actor. The background thread will be interrupted, unprocessed messages will be passed to the afterStop
     * method, if exists.
     * Has no effect if the Actor is not started.
     * @return The actor
     */
    public final Actor stop() {
        synchronized (lock) {
            if (!indicateStop()) {
                (codeReference.getAndSet(null)) ? actorAction(this) { throw TERMINATE } : currentAction.get()?.cancel()
            }
        }
        return this
    }

    /**
     * Checks the current status of the Actor.
     */
    public final boolean isActive() {
        return !stopFlag.get()
    }

    /**
     * Checks whether the current thread is the actor's current thread.
     */
    public final boolean isActorThread() {
        return Thread.currentThread() == currentAction.get()?.actionThread
    }

    /**
     * This method represents the body of the actor. It is called upon actor's start and can exit either normally
     * by return or due to actor being stopped through the stop() method, which cancels the current actor action.
     * Provides an extension point for subclasses to provide their custom Actor's message handling code.
     * The default implementation throws UnsupportedOperationException.
     */
    protected void act() {
        throw new UnsupportedOperationException("The act() method must be overriden")
    }

    /**
     * Schedules an ActorAction to take the next message off the message queue and to pass it on to the supplied closure.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     * @param duration Time to wait at most for a message to arrive. The actor terminates if a message doesn't arrive within the given timeout.
     * The TimeCategory DSL to specify timeouts is available inside the Actor's act() method.
     * @param code The code to handle the next message. The reply() and replyIfExists() methods are available inside
     * the closure to send a reply back to the actor, which sent the original message.
     */
    protected final void react(final Duration duration, final Closure code) {
        react(duration.toMilliseconds(), code)
    }

    /**
     * Schedules an ActorAction to take the next message off the message queue and to pass it on to the supplied closure.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     * @param code The code to handle the next message. The reply() and replyIfExists() methods are available inside
     * the closure to send a reply back to the actor, which sent the original message.
     */
    protected final void react(final Closure code) {
        react(0, code)
    }

    /**
     * Schedules an ActorAction to take the next message off the message queue and to pass it on to the supplied closure.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     * @param timeout Time in miliseconds to wait at most for a message to arrive. The actor terminates if a message doesn't arrive within the given timeout.
     * @param code The code to handle the next message. The reply() and replyIfExists() methods are available inside
     * the closure to send a reply back to the actor, which sent the original message.
     */
    protected final void react(final long timeout, final Closure code) {

        int maxNumberOfParameters = code.maximumNumberOfParameters
        //todo handle timeout - test
        //todo check reply on all messages and the actor - test
        //todo test and document that reply on the actor goes to the last message

        Closure reactCode = {List<ActorMessage> messages ->
            if (messages.any {ActorMessage actorMessage -> actorMessage.payLoad == TIMEOUT}) throw TIMEOUT
            ReplyEnhancer.enhanceWithReplyMethods(this, messages)
            code.call(*(messages*.payLoad))
            this.repeatLoop()
        }

        synchronized (lock) {
            if (stopFlag.get()) throw TERMINATE
            assert (codeReference.get() == null), "Cannot have more react called at the same time."

            bufferedMessages = new MessageHolder(maxNumberOfParameters)

            ActorMessage currentMessage
            while((!bufferedMessages.ready) && (currentMessage = (ActorMessage)messageQueue.poll())) {
                bufferedMessages.addMessage(currentMessage)
            }
            if (bufferedMessages.ready) {
                final List<ActorMessage> messages = bufferedMessages.messages
                actorAction(this) { reactCode.call(messages) }
                bufferedMessages = null
            } else {
                codeReference.set(reactCode)
                if (timeout > 0) {
                    timerTask.set([run: { this.send(TIMEOUT) }] as TimerTask)
                    timer.schedule(timerTask.get(), timeout)
                }
            }
        }
        throw CONTINUE
    }

    /**
     * Adds a message to the Actor's queue. Can only be called on a started Actor.
     * If there's no ActorAction scheduled for the actor a new one is created and scheduled on the thread pool.
     */
    public final Actor send(Object message) {
        synchronized (lock) {
            checkState()
            cancelCurrentTimeoutTimer(message)

            def actorMessage = ActorMessage.build(message)

            final Closure currentReference = codeReference.get()
            if (currentReference) {
                assert bufferedMessages && !bufferedMessages.isReady()
                bufferedMessages.addMessage actorMessage
                if (bufferedMessages.ready) {
                    final List<ActorMessage> messages = bufferedMessages.messages
                    actorAction(this) { currentReference.call(messages) }
                    codeReference.set(null)
                    bufferedMessages = null
                }
            } else {
                messageQueue.put(actorMessage)
            }
        }
        return this
    }

    /**
     * Adds a message to the Actor's queue. Can only be called on a started Actor.
     * If there's no ActorAction scheduled for the actor a new one is created and scheduled on the thread pool.
     */
    public final Actor leftShift(Object message) { send message }

    /**
     * Clears the message queue returning all the messages it held.
     * @return The messages stored in the queue
     */
    final List sweepQueue() {
        def messages = []
        Object message = messageQueue.poll()
        while (message != null) {
            messages << message
            message = messageQueue.poll()
        }
        return messages
    }

    /**
     * Ensures that the suplied closure will be invoked repeatedly in a loop.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     * @param code The closure to invoke repeatedly
     */
    protected final void loop(final Closure code) {
        assert loopCode.get() == null, "The loop method must be only called once"
        loopCode.set(code)
        doLoopCall(code)
    }

    /**
     * Plans another loop iteration
     */
    protected final void repeatLoop() {
        final Closure code = loopCode.get()
        if (!code) return;
        doLoopCall(code)
    }

    private def doLoopCall(Closure code) {
        if (stopFlag.get()) throw TERMINATE
        actorAction(this, code)
        throw CONTINUE
    }

    private def cancelCurrentTimeoutTimer(message) {
        if (message != TIMEOUT) timerTask.get()?.cancel()
    }

    /**
     * Checks, whether the Actor is active.
     * @throws IllegalStateException If the Actor is not active.
     */
    private void checkState() {
        if (stopFlag.get()) throw new IllegalStateException("The actor hasn't been started.");
    }

    //Document before next release
    //todo document send operator
    //todo add reply() and replyIfExists() to messages
    //todo implement reply for thread-bound actors and between the two actor categories
    //todo use Gradle
    //todo introduce actor groups - actors sharing a thread pool

    //Planned for the next release
    //todo reconsider reply() and replyIfExists()
    //todo multiple messages in receive() and react()
    //todo system properties for the default pool size
    //todo thread-bound actors could use threads from a pool or share a thread factory

    //Backlog
    //todo use AST transformation to turn actors methods into async processing
    //todo consider asynchronous metaclass
    //todo create a performance benchmark
    //todo implement in Java
    //todo unify actors and pooled actors behavior on timeout and exception, (retry after timeout and exception or stop)
    //todo try the fixes for the MixinTest
    //todo consider flow control to throttle message production
    //todo resize the pool if all threads are busy or blocked
    //todo maven
    //todo put into maven repo
    //todo add transitive mvn dependencies
    //todo support mixins for event-driven actors
    //todo consider other types of queues
    //todo use ForkJoin
    //todo add synchronous calls plus an operator

    //To consider
    //todo shorten method names withAsynchronizer and withParallelizer doAsync, doParallel
    //todo add sendLater(Duration) and sendAfterDone(Future)
    //todo consider pass by copy (clone, serialization) for mutable messages, reject mutable messages otherwise
    //todo unify and publish spawn operation and mail boxes
    //todo associate a mail box with each thread, not only with actors
    //todo add generics to actors
    //todo implement remote actors

    /*
import scala.actors.Actor._
import scala.actors.Future

case class Fib(n: Int)
case class Add(a: Future[Int], b: Future[Int])
case class Add2(a: Int, b: Future[Int])

val fib = actor { loop { react {
 case Fib(n) if n <= 2 => reply(1)
 case Fib(n) =>
   val a = self !! (Fib(n-1), { case x => x.asInstanceOf[Int] })
   val b = self !! (Fib(n-2), { case x => x.asInstanceOf[Int] })
   self.forward(Add(a, b))
 case Add(a, b) if a.isSet && b.isSet =>
   reply(a() + b())
 case Add(a, b) if a.isSet =>
   self.forward(Add2(a(), b))
 case Add(a, b) if b.isSet =>
   self.forward(Add2(b(), a))
 case Add(a, b) =>
   self.forward(Add(a, b))
 case Add2(a, b) if b.isSet =>
   reply(a + b())
 case Add2(a, b) =>
   self.forward(Add2(a, b))
} } }
     */
}