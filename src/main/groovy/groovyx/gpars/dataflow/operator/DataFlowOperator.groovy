//  GPars (formerly GParallelizer)
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

package groovyx.gpars.dataflow.operator

import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.PooledActorGroup
import groovyx.gpars.actor.ActorGroup

/**
 * @author Vaclav Pech
 * Date: Sep 9, 2009
 */
public final class DataFlowOperator {

  private static final dfOperatorActorGroup = new PooledActorGroup()


  public static DataFlowOperator operator(final Map channels, final Closure code) {
    return new DataFlowOperator(channels, code).start(dfOperatorActorGroup)
  }

  public static DataFlowOperator operator(final Map channels, final ActorGroup group, final Closure code) {
    return new DataFlowOperator(channels, code).start(group)
  }

  private final List inputs
  private final List outputs
  private final Closure code
  private final Actor actor

  public def DataFlowOperator(final Map channels, final Closure code) {
    this.inputs = channels.inputs.asImmutable()
    this.outputs = channels.outputs.asImmutable()
    this.code = code.clone()
    this.code.delegate = this
  }

  private DataFlowOperator start(ActorGroup group) {
    actor = group.actor {
      loop {
        inputs.eachWithIndex {input, index -> input.getValAsync(index, actor)}
        def values = [:]
        handleValueMessage(values, inputs.size())
      }
    }
    actor.start()
    return this
  }

  //todo test and document async val for actors
  //todo check whether the number of parameters and input channels match
  //todo think of ways to sync with operators - join, stop, implement actor
  //todo groups
  //todo interruptions
  //todo Non-blocking DFStream
  //todo test with limited number of threads
  //todo docs

  private void handleValueMessage(Map values, count) {
    if (values.size() < count) {
      actor.react {
        values[it.attachment] = it.result
        handleValueMessage(values, count)
      }
    } else {
      def results = values.sort {it.key}.values() as List
      code.call(* results)
    }
  }

  public void stop() {
    actor.stop()
  }

  private Map bindOutput(final int idx, final value) {
    outputs[idx] << value
  }
}