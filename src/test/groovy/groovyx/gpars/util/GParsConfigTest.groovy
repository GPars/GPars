// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2013  The original author or authors
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
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.group.NonDaemonPGroup
import groovyx.gpars.scheduler.Pool
import groovyx.gpars.scheduler.ResizeablePool
import spock.lang.Specification

class GParsConfigTest extends Specification {
    def "without initialization the config values are empty"() {
        expect:
        GParsConfig.poolFactory == null
        GParsConfig.retrieveDefaultPool() != null
        GParsConfig.timerFactory == null
    }

    def "default pool should be used for default parallel groups"() {
        given:
        def myPool = new ResizeablePool(true)

        def factory = new PoolFactory() {
            @Override
            Pool createPool() {
                return myPool
            }

            @Override
            Pool createPool(final boolean daemon) {
                return myPool
            }

            @Override
            Pool createPool(final int numberOfThreads) {
                return myPool
            }

            @Override
            Pool createPool(final boolean daemon, final int numberOfThreads) {
                return myPool
            }
        }
        when:
        GParsConfig.poolFactory = factory

        then:
        GParsConfig.retrieveDefaultPool().is(myPool)
        GParsConfig.poolFactory.is(factory)

        cleanup:
        GParsConfig.poolFactoryFlag = false
        GParsConfig.poolFactory = null
        GParsConfig.poolFactoryFlag = false
    }

    def "default pool should be used in parallel groups"() {
        given:
        def myPool = new ResizeablePool(true)

        def factory = new PoolFactory() {
            @Override
            Pool createPool() {
                return myPool
            }

            @Override
            Pool createPool(final boolean daemon) {
                return myPool
            }

            @Override
            Pool createPool(final int numberOfThreads) {
                return myPool
            }

            @Override
            Pool createPool(final boolean daemon, final int numberOfThreads) {
                return myPool
            }
        }
        when:
        GParsConfig.poolFactory = factory
        def group1 = new DefaultPGroup()
        def group2 = new DefaultPGroup(10)
        def group3 = new NonDaemonPGroup()
        def group4 = new NonDaemonPGroup(10)

        then:
        group1.threadPool.is(myPool)
        group2.threadPool.is(myPool)
        group3.threadPool.is(myPool)
        group4.threadPool.is(myPool)

        cleanup:
        GParsConfig.poolFactoryFlag = false
        GParsConfig.poolFactory = null
        GParsConfig.poolFactoryFlag = false
    }

    def "default timer factory should be retrieved"() {
        given:
        def myTimer = new GeneralTimer() {
            @Override
            void schedule(final Runnable task, final long timeout) {}

            @Override
            void shutdown() {

            }
        }

        def myTimerFactory = new TimerFactory() {
            @Override
            GeneralTimer createTimer(final String name, final boolean daemon) {
                return myTimer
            }
        }

        when:
        GParsConfig.timerFactory = myTimerFactory

        then:
        GParsConfig.retrieveDefaultTimer("", true).is(myTimer)

        cleanup:
        GParsConfig.timerFactoryFlag = false
        GParsConfig.timerFactory = null
        GParsConfig.timerFactoryFlag = false
    }
}
