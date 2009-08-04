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