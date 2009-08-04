package org.gparallelizer.samples.actors.safevariable

import org.gparallelizer.actors.pooledActors.SafeVariable

class Conference extends SafeVariable {
    def Conference() { super(0L) }
    private def register(long num) { data += num }
    private def unregister(long num) { data -= num }
}

final SafeVariable conference = new Conference()

final Thread t1 = Thread.start {
    conference << {register(10)}
}

final Thread t2 = Thread.start {
    conference << {register(5)}
}

final Thread t3 = Thread.start {
    conference << {unregister(3)}
}

[t1, t2, t3]*.join()

assert 12 == conference.val

