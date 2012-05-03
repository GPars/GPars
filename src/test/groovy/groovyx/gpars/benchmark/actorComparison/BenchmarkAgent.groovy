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

import groovyx.gpars.agent.Agent
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.scheduler.FJPool

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

final def concurrencyLevel = 8

4.times {
    group = new DefaultPGroup(new FJPool(concurrencyLevel))
    final def t1 = System.currentTimeMillis()
    final def cdl = new CountDownLatch(20000 * 500)
    def last = null

    for (int i = 0; i < 20000; i++) {
        final def channel = new Agent(new AgentState(last, cdl))
        channel.attachToThreadPool group.threadPool
//    channel.makeFair()
        last = channel
    }

    Closure message
    message = {it.relay(message)}

    for (int i = 0; i < 500; i++) {
        last.send(message)
    }

    cdl.await(1000, TimeUnit.SECONDS)

    group.shutdown()
    final def t2 = System.currentTimeMillis()
    println(t2 - t1)
}

final class AgentState {

    private final def soFarLast
    private final def cdl

    def AgentState(final def soFarLast, final def cdl) {
        this.soFarLast = soFarLast
        this.cdl = cdl
    }

    def relay(final def msg) {
        soFarLast?.send(msg)
        cdl.countDown()
    }
}