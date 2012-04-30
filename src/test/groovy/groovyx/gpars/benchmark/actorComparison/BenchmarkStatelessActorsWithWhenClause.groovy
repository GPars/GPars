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
import groovyx.gpars.scheduler.DefaultPool

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

final def concurrencyLevel = 20
group = new DefaultPGroup(new DefaultPool(false, concurrencyLevel))

final def t1 = System.currentTimeMillis()
final def cdl = new CountDownLatch(10000 * 500)
def last = null

int i = 0
while (i < 10000) {
    def soFarLast = last
    def channel = group.messageHandler {
        when {def msg ->
            soFarLast?.send(msg)
            cdl.countDown()
        }
    }
    last = channel
    i += 1
}

i = 0
while (i < 500) {
    last.send("Hi")
    i += 1
}

cdl.await(1000, TimeUnit.SECONDS)

group.shutdown()
final def t2 = System.currentTimeMillis()
println(t2 - t1)
