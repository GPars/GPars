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

package groovyx.gpars.samples.actors.safe

import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.Safe

/**
 * A copy strategy to create a safe copy when someone reads the internal state
 */
final Closure cl = {
    it ? new LinkedList(it) : null
}

/**
 * Creating a Safe around a single-element list, with custom copy strategy.
 */
final Safe<List> agent = new Safe<List>([1], cl)

agent << {it << 2}      //add 2 to the list
agent << {println it}   //print the state [1, 2]

println(agent.sendAndWait {it})         //The return value of the closure it sent back in reply
println(agent.sendAndWait {it.size()})  //The size of the internal list is sent back
println agent.val                       //The usual, lazy value retrieval
println agent.instantVal                //The immediate internal state snapshot retrieval
agent.valAsync {println it}             //The asynchronous variant
agent.await()                           //Waits until all messages currently in the queue get processed

agent << [1, 2, 3, 4, 5]                //Send a new array to set as the new internal state
println agent.val                       //Print the new state

agent << {delegate.stop()}              //One way to stop the Safe
agent.stop()                            //A better way to stop the Safe
agent.join()                            //Wait for the Safe to stop

