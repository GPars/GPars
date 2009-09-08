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

package org.gparallelizer.samples.dataflow

import org.gparallelizer.dataflow.DataFlowVariable
import static org.gparallelizer.dataflow.DataFlow.start
import org.gparallelizer.actors.pooledActors.PooledActors
import org.gparallelizer.actors.pooledActors.AbstractPooledActor

/**
 * Shows cooperation between an actor and a dataflow thread.
 * Since dataflow threads are plain pooled actors, they can react to messages just like actors do.
 */

final DataFlowVariable a = new DataFlowVariable()

final AbstractPooledActor doubler = PooledActors.actor {
    react {
        a << 2 * it
    }
}.start()

final AbstractPooledActor thread = start {
    react {
        doubler << it  //send a number to the doubler
        println "Result ${a.val}"  //wait for the result to be bound to 'a'
    }
}

thread << 10

System.in.read()
System.exit 0 
