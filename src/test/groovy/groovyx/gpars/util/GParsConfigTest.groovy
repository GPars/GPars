// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
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

package groovyx.gpars.util

import groovyx.gpars.GParsConfig
import groovyx.gpars.GParsExecutorsPoolUtil
import groovyx.gpars.GParsPoolUtil
import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.Actors
import groovyx.gpars.dataflow.Dataflow
import groovyx.gpars.scheduler.ResizeablePool
import spock.lang.Specification

class GParsConfigTest extends Specification {
    def "default pool should be used"() {
        given:
        final myPool = new ResizeablePool(true)

        when:
        GParsConfig.defaultPool = myPool

        then:
        Actors.defaultActorPGroup.threadPool.is(myPool)
        Dataflow.DATA_FLOW_GROUP.threadPool.is(myPool)
    }

    def "default timer factory should be used"() {
        given:
        final myTimer = new Timer()
        final myTimerFactory = new TimerFactory() {
            @Override
            Timer createTimer(final String name, final boolean daemon) {
                return myTimer
            }
        }

        when:
        GParsConfig.timerFactory = myTimerFactory

        then:
        Actor.timer.is(myTimer)
        GParsExecutorsPoolUtil.timer.is(myTimer)
        GParsPoolUtil.timer.is(myTimer)
    }

}
