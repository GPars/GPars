package org.gparallelizer.samples.actors.safevariable

import org.gparallelizer.actors.pooledActors.SafeVariable

final SafeVariable counter = new SafeVariable<Long>(0L)

final Thread t1 = Thread.start {
    counter << {updateValue it + 1}
}

final Thread t2 = Thread.start {
    counter << {updateValue it + 6}
}

final Thread t3 = Thread.start {
    counter << {updateValue it - 2}
}

[t1, t2, t3]*.join()

assert 5 == counter.val