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

package groovyx.gpars.samples.agent

import groovyx.gpars.actor.Actors
import groovyx.gpars.agent.Agent

def name = new Agent<String>()           //new Agent

name << {updateValue 'Joe' }            //Set the state to 'Joe'
name << {updateValue(it + ' and Dave')} //Set the state to a new value derived from the previous value
println name.val
println(name.sendAndWait({it.size()}))

name << 'Alice'                         //Set a new state
println name.val
name.valAsync {println "Async: $it"}

name << 'James'                         //Set a new state
println name.val

Actors.actor {                          //Create a new actor to communicate with the Agent
    name << {owner.send it.toUpperCase()}          //Construct an upper cased string and reply it back. The internal state of the Agent doesn't change here
    react {                             //Wait for the reply with the uppercase string
        println it
    }
}.join()                        //Start and wait for termination of the actor

name.await()
