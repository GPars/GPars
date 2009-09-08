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

actor.join()

sampleGroup.shutdown()
Actors.defaultActorGroup.shutdown()
