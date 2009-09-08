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

package org.gparallelizer.samples.actors.safevariable

import org.gparallelizer.actors.pooledActors.SafeVariable
import org.gparallelizer.actors.pooledActors.PooledActors

final Closure cl = {
    it ? new LinkedList(it) : null
}

final SafeVariable<List> agent = new SafeVariable<List>([1], cl)

agent << {it << 2}
agent << {println it}

println (agent.sendAndWait {it})
println (agent.sendAndWait {it.size()})
println agent.val

agent << [1, 2, 3, 4, 5]
println agent.val

agent << {delegate.stop()}
agent.stop()
agent.join()


def name = new SafeVariable<String>()

name << {updateValue 'Joe' }
name << {updateValue(it + ' and Dave')}
println name.val
println (name.sendAndWait({it.size()}))

name << 'Alice'
println name.val
name.valAsync {println "Async: $it"}

name << 'James'
println name.val

PooledActors.actor {
    name << {it.toUpperCase()}
    react {
        println it
    }
}.start().join()

name.stop()
name.join()
