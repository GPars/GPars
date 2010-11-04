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
import groovyx.gpars.actor.impl.ActorContinuationException;

/**
 * @author Vaclav Pech
 *         Date: Nov 4th 2010
 */

//todo javadoc
//todo conditional loops
//todo exceptions
//todo receive
//todo replies on objects
public class DefaultActor extends AbstractLoopingActor {

    private Closure nextContinuation;
    private Runnable loopCode;
    private Closure loopClosure;
    private Runnable startCode;
    private static final long serialVersionUID = -439517926332934061L;

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
            } else {
                if (message == START_MESSAGE) handleStartMessage();
                else
                    throw new IllegalStateException("The actor " + this + " cannot handle the message " + message + ", as it has no registered message handler at the moment.");
            }
        } catch (ActorContinuationException ignore) {
        }
        if (nextContinuation == null) {
            try {
                if (loopCode == null)
                    if (loopClosure == null) terminate();
                    else loopClosure.call();
                else loopCode.run();
            } catch (ActorContinuationException ignore) {
            }
        }
    }

    public final void loop(final Runnable code) {
        checkForNull(code);
        if (code instanceof Closure) {
            final Closure closure = (Closure) code;
            checkForBodyArguments(closure);
            final Closure enhancedClosure = enhanceClosure(closure);
            this.loopClosure = enhancedClosure;
            enhancedClosure.call();
        } else {
            this.loopCode = code;
            loopCode.run();
        }
    }

    public final void react(final Closure code) {
        checkForNull(code);
        checkForMessageHandlerArguments(code);
        nextContinuation = enhanceClosure(code);
//        throw ActorException.CONTINUE;
    }

    @Override
    protected final void handleStart() {
        send(START_MESSAGE);
    }

    @SuppressWarnings({"CatchGenericClass"})
    private void handleStartMessage() {
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
    private static final String START_MESSAGE = "Start";
}
