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

import groovyx.gpars.GParsPool
import groovyx.gpars.actor.Actors
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.scheduler.FJPool

import java.util.concurrent.CountDownLatch

final def concurrencyLevel = 10
final def numOfActors = 100
final def iterations = 1000
final def numOfMessages = 10
group = new DefaultPGroup(new FJPool(concurrencyLevel))
final def latch = new CountDownLatch(iterations * numOfMessages * numOfActors)

def createReactor(final code) {
    group.reactor code
//    group.fairReactor code
}

def reactors = (1..numOfActors).collect {
    createReactor {
        latch.countDown()
        it
    }
}


def controller = Actors.reactor {
    def t1 = System.nanoTime()

    iterations.times {
        GParsPool.withPool(10) {
            reactors.eachParallel {
                numOfMessages.times {msg ->
                    it.send(msg)
                }
            }
        }
    }

    latch.await()
    def t2 = System.nanoTime()
    println((t2 - t1) / 1000000)
    terminate()
}

controller 'Start'
controller.join()
group.shutdown()