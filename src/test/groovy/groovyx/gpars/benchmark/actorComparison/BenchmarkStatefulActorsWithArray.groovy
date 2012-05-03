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

import groovyx.gpars.actor.Actor
import groovyx.gpars.actor.DefaultActor
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.scheduler.DefaultPool

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

final def t1 = System.currentTimeMillis()

final def concurrencyLevel = 20
group = new DefaultPGroup(new DefaultPool(false, concurrencyLevel))
final def channels = new Actor[10000]

final def cdl = new CountDownLatch(10000 * 500)

for (int i = 0; i < 10000; i++) {
    final def channel = new DefaultActorHandlerWithArray(channels, i, cdl)
    channel.parallelGroup = group
    channels[i] = channel
    channel.start()
}

for (int i = 0; i < 500; i++) {
    channels[i].send("Hi")
    for (int j = 0; j < i; j++) {
        cdl.countDown()
    }
}

cdl.await(1000, TimeUnit.SECONDS)

group.shutdown()
final def t2 = System.currentTimeMillis()
println(t2 - t1)

final class DefaultActorHandlerWithArray extends DefaultActor {

    final def channels
    private final def index
    private final def cdl

    def DefaultActorHandlerWithArray(final def channels, final def index, final def cdl) {
        this.channels = channels
        this.index = index
        this.cdl = cdl
//        makeFair()
    }

    void act() {
        loop {
            react {msg ->
                if (index < channels.length - 1) channels[index + 1].send(msg)
                cdl.countDown()
            }
        }
    }
}