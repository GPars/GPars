package org.gparallelizer.samples

import org.gparallelizer.actors.pooledActors.AbstractPooledActor
import org.gparallelizer.actors.pooledActors.PooledActor
import org.gparallelizer.actors.pooledActors.PooledActorGroup
import org.gparallelizer.actors.pooledActors.PooledActors

/**
 * Three pooled actors are created, two of them in a newly created pooled actor group, one in the fefault pooled actor
 * group using the factory method of the PooledActors class.
 * @author Vaclav Pech
 */

final PooledActorGroup sampleGroup = new PooledActorGroup()

println "Sample Group $sampleGroup"
println "Default group ${PooledActors.defaultPooledActorGroup}"
println ""

sampleGroup.actor {
    println ((actorGroup==PooledActors.defaultPooledActorGroup) ? "I am in the default pooled group" : "I am in the sample pooled group")
}.start()

Thread.sleep 1000

class GroupSamplePooledActor extends AbstractPooledActor {

    protected void act() {
        println ((actorGroup==PooledActors.defaultPooledActorGroup) ? "I am in the default pooled group" : "I am in the sample pooled group")
    }
}

new GroupSamplePooledActor().start()

Thread.sleep 1000

final PooledActor actor = new GroupSamplePooledActor()
actor.actorGroup = sampleGroup
actor.start()
