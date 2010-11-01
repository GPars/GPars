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

package groovyx.gpars.benchmark

import groovyx.gpars.actor.Actors
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.scheduler.DefaultPool

final def concurrencyLevel = 20
group = new DefaultPGroup(new DefaultPool(false, concurrencyLevel))

def createReactor(final code) {
    group.reactor code
}

def reactors = (1..concurrencyLevel).collect {createReactor({it})}

def iterations = 1000
def numOfMessages = 1000

def controller = Actors.reactor {
    def t1 = System.nanoTime()

    iterations.times {
        for (reactor in reactors) {
            for (msg in numOfMessages) {
                reactor.send(msg)
            }
        }
    }

    def t2 = System.nanoTime()
    println((t2 - t1) / 1000000)
    terminate()
}

controller 'Start'
controller.join()
group.shutdown()