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

package groovyx.gpars.benchmark.actorComparison

import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.scheduler.FJPool

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

final def t1 = System.currentTimeMillis()

final def numOfAttackers = 100000
final def attackerConcurrencyLevel = 7
final def attackGroup = new DefaultPGroup(new FJPool(attackerConcurrencyLevel))
final def attackSignal = new CountDownLatch(1)
final def retreatSignal = new CountDownLatch(numOfAttackers)

final def defendGroup = new DefaultPGroup(new FJPool(1))
final def defender = new StressHandler()
defender.parallelGroup = defendGroup
defender.start()

int i = 0
while (i < numOfAttackers) {
    final def weapon = i
    def attacker
    attacker = attackGroup.actor {
        attackSignal.await()
        defender weapon
        attacker.react {
            retreatSignal.countDown()
        }
    }
    i += 1
}

attackSignal.countDown()
retreatSignal.await(1000, TimeUnit.SECONDS)

attackGroup.shutdown()
defendGroup.shutdown()
final def t2 = System.currentTimeMillis()
println(t2 - t1)
