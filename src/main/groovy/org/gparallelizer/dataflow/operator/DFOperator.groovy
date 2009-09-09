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

package org.gparallelizer.dataflow.operator

import org.gparallelizer.actors.pooledActors.PooledActors
import org.gparallelizer.actors.pooledActors.PooledActorGroup
import org.gparallelizer.actors.Actor

/**
 * @author Vaclav Pech
 * Date: Sep 9, 2009
 */
public final class DFOperator {

    private static final dfOperatorActorGroup = new PooledActorGroup()

    public static DFOperator operator(final Map channels, final Closure code) {
        return new DFOperator(channels, code).start()
    }

    private final List inputs
    private final List outputs
    private final Closure code
    private final Actor actor

    public def DFOperator(final Map channels, final Closure code) {
        this.inputs = channels.inputs.asImmutable()
        this.outputs = channels.outputs.asImmutable()
        this.code = code.clone()
        this.code.delegate = this
    }

    private DFOperator start() {
        actor = dfOperatorActorGroup.actor {
            loop {
                process()
            }
        }.start()
        return this
    }

    public void stop() {
        actor.stop()
    }

    private Map bindOutput(final int idx, final value) {
        outputs[idx] << value
    }

    private void process() {
        def values = inputs*.val

        code.call(* values)
    }
}