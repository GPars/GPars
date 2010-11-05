// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.actor;

import groovy.lang.Closure;
import groovy.time.Duration;
import groovyx.gpars.actor.impl.ActorContinuationException;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * @author Vaclav Pech
 *         Date: Nov 4th 2010
 */

//todo javadoc
//todo conditional loops
//todo handle exceptions
//todo timeout - also for DDA and Reactor
//todo exceptions
//todo receive
//todo replies on objects
//todo deprecate AbstractPoolActor
//todo remove oldActor and deprecated classes - actors, exceptions
public class DefaultActor extends AbstractLoopingActor {

    private Closure nextContinuation;
    private Runnable loopCode;
    private Closure loopClosure;
    private Runnable startCode;
    private static final long serialVersionUID = -439517926332934061L;
    private Closure afterLoopCode;
    private Callable<Boolean> loopCondition;

    /**
     */
    public DefaultActor() {
        this(null);
    }

    /**
     * @param code
     */
    public DefaultActor(final Runnable code) {
        if (code != null) {
            if (code instanceof Closure) checkForBodyArguments((Closure) code);
            startCode = code;
        }
        initialize(new DefaultActorClosure(this));
    }

    protected void act() {
        throw new UnsupportedOperationException("The act method has not been overridden");
    }

    final void onMessage(final Object message) {
        try {
            if (nextContinuation != null) {
                final Closure closure = nextContinuation;
                nextContinuation = null;
                closure.call(message);
            } else
                throw new IllegalStateException("The actor " + this + " cannot handle the message " + message + ", as it has no registered message handler at the moment.");
        } catch (ActorContinuationException ignore) {
        }
        if (nextContinuation == null) {
            try {
                if (loopCondition == null || loopCondition.call()) {
                    if (loopCode == null)
                        if (loopClosure == null) terminate();
                        else loopClosure.call();
                    else loopCode.run();
                } else {
                    if (afterLoopCode != null) {
                        loopCondition = null;
                        afterLoopCode.call();
                    }
                    terminate();
                }
            } catch (ActorContinuationException ignore) {
            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    /**
     * Ensures that the supplied closure will be invoked repeatedly in a loop.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     *
     * @param code The closure to invoke repeatedly
     */
    public final void loop(final Runnable code) {
        loop((Callable<Boolean>) null, null, code);
    }

    /**
     * Ensures that the supplied closure will be invoked repeatedly in a loop.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     *
     * @param numberOfLoops The loop will only be run the given number of times
     * @param code          The closure to invoke repeatedly
     */
    protected final void loop(final int numberOfLoops, final Runnable code) {
        loop(numberOfLoops, null, code);
    }

    /**
     * Ensures that the supplied closure will be invoked repeatedly in a loop.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     *
     * @param numberOfLoops The loop will only be run the given number of times
     * @param afterLoopCode Code to run after the main actor's loop finishes
     * @param code          The closure to invoke repeatedly
     */
    protected final void loop(final int numberOfLoops, final Closure afterLoopCode, final Runnable code) {
        loop(new Callable<Boolean>() {
            private int counter = 0;

            @Override
            public Boolean call() {
                counter++;
                //noinspection UnnecessaryBoxing
                return Boolean.valueOf(counter <= numberOfLoops);
            }
        }, afterLoopCode, code);
    }

    /**
     * Ensures that the supplied closure will be invoked repeatedly in a loop.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     *
     * @param condition A condition to evaluate before each iteration starts. If the condition returns false, the loop exits.
     * @param code      The closure to invoke repeatedly
     */
    protected final void loop(final Closure condition, final Runnable code) {
        loop(condition, null, code);

    }

    /**
     * Ensures that the supplied closure will be invoked repeatedly in a loop.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     *
     * @param condition     A condition to evaluate before each iteration starts. If the condition returns false, the loop exits.
     * @param afterLoopCode Code to run after the main actor's loop finishes
     * @param code          The closure to invoke repeatedly
     */
    protected final void loop(final Closure condition, final Closure afterLoopCode, final Runnable code) {
        loop(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return (Boolean) condition.call();
            }
        }, afterLoopCode, code);

    }

    /**
     * Ensures that the supplied closure will be invoked repeatedly in a loop.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     *
     * @param condition     A condition to evaluate before each iteration starts. If the condition returns false, the loop exits.
     * @param afterLoopCode Code to run after the main actor's loop finishes
     * @param code          The closure to invoke repeatedly
     */
    @SuppressWarnings({"OverlyComplexBooleanExpression"})
    private void loop(final Callable<Boolean> condition, final Closure afterLoopCode, final Runnable code) {
        checkForNull(code);

        if (afterLoopCode != null) {
            this.afterLoopCode = enhanceClosure(afterLoopCode);
        }
        loopCondition = condition;

        try {
            if (code instanceof Closure) {
                final Closure closure = (Closure) code;
                checkForBodyArguments(closure);
                final Closure enhancedClosure = enhanceClosure(closure);
                this.loopClosure = enhancedClosure;

                assert nextContinuation == null;
                while (!hasBeenStopped() && nextContinuation == null && (loopCondition == null || loopCondition.call())) {
                    enhancedClosure.call();
                }
            } else {
                this.loopCode = code;
                assert nextContinuation == null;
                while (!hasBeenStopped() && nextContinuation == null && (loopCondition == null || loopCondition.call())) {
                    loopCode.run();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    /**
     * Schedules an ActorAction to take the next message off the message queue and to pass it on to the supplied closure.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     *
     * @param code The code to handle the next message. The reply() and replyIfExists() methods are available inside
     *             the closure to send a reply back to the actor, which sent the original message.
     */
    public final void react(final Closure code) {
        react(-1L, code);
    }

    /**
     * Schedules an ActorAction to take the next message off the message queue and to pass it on to the supplied closure.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     *
     * @param duration Time to wait at most for a message to arrive. The actor terminates if a message doesn't arrive within the given timeout.
     *                 The TimeCategory DSL to specify timeouts must be enabled explicitly inside the Actor's act() method.
     * @param code     The code to handle the next message. The reply() and replyIfExists() methods are available inside
     *                 the closure to send a reply back to the actor, which sent the original message.
     */
    protected final void react(final Duration duration, final Closure code) {
        react(duration.toMilliseconds(), code);
    }

    /**
     * Schedules an ActorAction to take the next message off the message queue and to pass it on to the supplied closure.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     *
     * @param timeout  Time in milliseconds to wait at most for a message to arrive. The actor terminates if a message doesn't arrive within the given timeout.
     * @param timeUnit a TimeUnit determining how to interpret the timeout parameter
     * @param code     The code to handle the next message. The reply() and replyIfExists() methods are available inside
     *                 the closure to send a reply back to the actor, which sent the original message.
     */
    protected final void react(final long timeout, final TimeUnit timeUnit, final Closure code) {
        react(timeUnit.toMillis(timeout), code);
    }

    /**
     * Schedules an ActorAction to take the next message off the message queue and to pass it on to the supplied closure.
     * The method never returns, but instead frees the processing thread back to the thread pool.
     * Also adds reply() and replyIfExists() methods to the currentActor and the message.
     * These methods will call send() on the target actor (the sender of the original message).
     * The reply()/replyIfExists() methods invoked on the actor will be sent to all currently processed messages,
     * reply()/replyIfExists() invoked on a message will send a reply to the sender of that particular message only.
     *
     * @param timeout Time in milliseconds to wait at most for a message to arrive. The actor terminates if a message doesn't arrive within the given timeout.
     * @param code    The code to handle the next message. The reply() and replyIfExists() methods are available inside
     *                the closure to send a reply back to the actor, which sent the original message.
     */
    protected final void react(final long timeout, final Closure code) {
        if (!isActorThread()) {
            throw new IllegalStateException("Cannot call react from thread which is not owned by the actor");
        }
        checkForNull(code);
        checkForMessageHandlerArguments(code);
        nextContinuation = enhanceClosure(code);
//        throw ActorException.CONTINUE;
    }

    @Override
    public Actor silentStart() {
        throw new UnsupportedOperationException("Old actors cannot start silently. Use DefaultActor instead.");
    }

    @Override
    protected void handleStart() {
        super.handleStart();
        try {
            if (startCode != null) {
                if (startCode instanceof Closure) {
                    final Closure closure = enhanceClosure((Closure) startCode);
                    closure.call();
                } else {
                    startCode.run();
                }
            } else act();
            if (nextContinuation == null) terminate();
        } catch (IllegalStateException e) {
            terminate();
            throw e;
        } finally {
            startCode = null;
        }
    }

    private Closure enhanceClosure(final Closure closure) {
        final Closure cloned = (Closure) closure.clone();
        cloned.setResolveStrategy(Closure.DELEGATE_FIRST);
        cloned.setDelegate(this);
        return cloned;
    }

    private static void checkForNull(final Runnable code) {
        if (code == null)
            throw new IllegalArgumentException("An actor's message handlers and loops cannot be set to a null value.");
    }

    private static void checkForBodyArguments(final Closure closure) {
        if (closure.getMaximumNumberOfParameters() > 1)
            throw new IllegalArgumentException("An actor's body as well as a body of a loop can only expect 0 arguments. " + closure.getMaximumNumberOfParameters() + EXPECTED);
    }

    private static void checkForMessageHandlerArguments(final Closure code) {
        if (code.getMaximumNumberOfParameters() > 1)
            throw new IllegalArgumentException("An actor's message handler can only expect 0 or 1 argument. " + code.getMaximumNumberOfParameters() + EXPECTED);
    }

    private static final String EXPECTED = " expected.";
}
