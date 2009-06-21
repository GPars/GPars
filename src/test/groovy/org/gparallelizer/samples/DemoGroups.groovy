package org.gparallelizer.samples

import org.gparallelizer.actors.*

/**
 * Three thread-bound actors are created, two of them in a newly created actor group, one in the fefault actor group using
 * the factory method of the Actors class.
 * @author Vaclav Pech
 */

final AbstractThreadActorGroup sampleGroup = new ThreadActorGroup()

println "Sample Group $sampleGroup"
println "Default group ${Actors.defaultActorGroup}"
println ""

sampleGroup.oneShotActor {
    println ((actorGroup==Actors.defaultActorGroup) ? "I am in the default group" : "I am in the sample group")
}.start()

Thread.sleep 1000

class GroupSampleActor extends DefaultThreadActor {

    protected void act() {
        println ((actorGroup==Actors.defaultActorGroup) ? "I am in the default group" : "I am in the sample group")
        stop()
    }
}

new GroupSampleActor().start()

Thread.sleep 1000

final Actor actor = new GroupSampleActor()
actor.actorGroup = sampleGroup
actor.start()

System.in.read()

sampleGroup.shutdown()
Actors.defaultActorGroup.shutdown()
