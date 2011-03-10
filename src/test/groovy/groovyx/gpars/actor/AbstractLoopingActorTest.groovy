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

package groovyx.gpars.actor;


import groovyx.gpars.group.DefaultPGroup
import spock.lang.Specification

class AbstractLoopingActorTest extends Specification {
    def "changing parallel group on a DDA and reactor changes the core thread pool"() {
        given:
        def group = new DefaultPGroup()

        when:
        actor.parallelGroup = group
        then:
        actor.core.threadPool == group.threadPool

        cleanup:
        group.shutdown()

        where:
        actor << [new DynamicDispatchActor(), new ReactiveActor({}), Actors.messageHandler {}, Actors.reactor {}]
    }
}
