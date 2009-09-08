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

package org.gparallelizer.samples.actors

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
