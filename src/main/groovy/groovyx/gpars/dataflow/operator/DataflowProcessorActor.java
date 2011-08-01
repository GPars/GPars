// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
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

package groovyx.gpars.dataflow.operator;

import groovy.lang.Closure;
import groovyx.gpars.actor.StaticDispatchActor;
import groovyx.gpars.actor.impl.MessageStream;
import groovyx.gpars.group.PGroup;

import java.util.List;


/**
 * A base actor class for operators' and selectors' actors
 *
 * @author Vaclav Pech
 */
abstract class DataflowProcessorActor extends StaticDispatchActor<Object> {
    protected final List inputs;
    protected final List outputs;
    protected final Closure code;
    protected final DataflowProcessor owningProcessor;
    protected boolean stoppingGently = false;

    DataflowProcessorActor(final DataflowProcessor owningProcessor, final PGroup group, final List outputs, final List inputs, final Closure code) {
        super();
        parallelGroup = group;

        this.owningProcessor = owningProcessor;
        this.outputs = outputs;
        this.inputs = inputs;
        this.code = code;
    }

    /**
     * Sends the message, ignoring exceptions caused by the actor not being active anymore
     *
     * @param message The message to send
     * @return The current actor
     */
    @Override
    public MessageStream send(final Object message) {
        try {
            super.send(message);
        } catch (IllegalStateException e) {
            if (!hasBeenStopped()) throw e;
        }
        return this;
    }

    /**
     * All messages unhandled by sub-classes will result in an exception being thrown
     *
     * @param message The unhandled message
     */
    @Override
    public void onMessage(final Object message) {
        throw new IllegalStateException("The dataflow actor doesn't recognize the message $message");
    }

    /**
     * Handles the poisson message.
     * After receiving the poisson a dataflow operator will send the poisson to all its output channels and terminate.
     *
     * @param data The poisson to re-send
     *             return True, if poisson has been received
     */
    boolean checkPoison(final Object data) {
        if (data instanceof PoisonPill) {
            owningProcessor.bindAllOutputsAtomically(data);
            owningProcessor.terminate();
            return true;
        }
        return false;
    }

    final void reportException(final Throwable e) {
        owningProcessor.reportError(e);
    }
}
