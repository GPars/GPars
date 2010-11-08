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

package groovyx.gpars.samples.actors.stateful

import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.DefaultActor
import groovyx.gpars.group.DefaultPGroup

/**
 * Three actors are created, two of them in a newly created actor group, one in the default actor
 * group using the factory method of the Actors class.
 * @author Vaclav Pech
 */

final DefaultPGroup sampleGroup = new DefaultPGroup()

println "Sample Group $sampleGroup"
println "Default group ${Actors.defaultActorPGroup}"
println ""

sampleGroup.actor {
    println((parallelGroup == Actors.defaultActorPGroup) ? "I am in the default pooled group" : "I am in the sample pooled group")
}

Thread.sleep 1000

class GroupSamplePooledActor extends DefaultActor {

    protected void act() {
        println((parallelGroup == Actors.defaultActorPGroup) ? "I am in the default pooled group" : "I am in the sample pooled group")
    }
}

new GroupSamplePooledActor().start()

Thread.sleep 1000

final Actor actor = new GroupSamplePooledActor()
actor.parallelGroup = sampleGroup
actor.start()
