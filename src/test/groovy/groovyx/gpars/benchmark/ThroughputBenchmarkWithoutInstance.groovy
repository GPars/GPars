// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2012  The original author or authors
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

import groovyx.gpars.actor.DynamicDispatchActor
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.scheduler.FJPool
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit




class ThroughputBenchmarkWithoutInstance {
    int repeatFactor = 2
    long repeat = 30000L * repeatFactor
    int maxClients = 4;
    int numberOfClients = 48;
    DefaultPGroup group = new DefaultPGroup(new FJPool(maxClients))
    public static final int RUN = 1
    public static final int MESSAGE = 2

    final int maxRunDurationMillis = 20000

    public void timeThroughput() {
        CountDownLatch latch = new CountDownLatch(numberOfClients)
        long repeatsPerClient = repeat / numberOfClients
        ArrayList<ClientInteger> clients = new ArrayList<ClientInteger>()     //put lists inside method
        ArrayList<DestinationInteger> destinations = new ArrayList<DestinationInteger>()

        (1..numberOfClients).each {
            destinations.add(new DestinationInteger(group).start())
        }
        destinations.each {destination ->
            clients.add(new ClientInteger(destination, latch, repeatsPerClient, group).start())
        }
        long start = System.nanoTime()
        clients.each {
            it.send(ThroughputBenchmarkWithoutInstance.RUN)
        }

        latch.await(maxRunDurationMillis, TimeUnit.MILLISECONDS)
        long durationNs = (System.nanoTime() - start)
        clients.each {
            it.terminate()
        }
        destinations.each {
            it.terminate();
        }
        println durationNs / 1000000 + " Dynamic Dispatch Without Instance";
    }




    static void main(String[] args) {
        ThroughputBenchmarkWithoutInstance tb = new ThroughputBenchmarkWithoutInstance()
        for (int i = 0; i < 40;i++)
            tb.timeThroughput()
    }
}

class ClientInteger extends DynamicDispatchActor {

    long sent = 0L
    long received = 0L
    DestinationInteger actor
    long repeatsPerClient
    CountDownLatch latch


    public ClientInteger(DestinationInteger actor, CountDownLatch latch, long repeatsPerClient, DefaultPGroup group) {
        this.parallelGroup = group
        this.actor = actor
        this.repeatsPerClient = repeatsPerClient
        this.latch = latch
    }

    void onMessage(final Object msg) {
        if (ThroughputBenchmarkWithoutInstance.MESSAGE.equals(msg)) {
            received += 1
            if (sent < repeatsPerClient) {
                actor.send(msg)
                sent += 1
            } else if (received >= repeatsPerClient) {
                latch.countDown()

            }
        }
        if (ThroughputBenchmarkWithoutInstance.RUN.equals(msg)) {
            (1..Math.min(repeatsPerClient, 1000L)).each {
                actor.send(ThroughputBenchmarkWithoutInstance.MESSAGE)
                sent += 1
            }
        }
    }
}

class DestinationInteger extends DynamicDispatchActor {
    public DestinationInteger(DefaultPGroup group) {
        this.parallelGroup = group
    }

    void onMessage(final Object msg) {
        getSender().send(msg)
    }

}

