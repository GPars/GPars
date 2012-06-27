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

package groovyx.gpars.benchmark.dataflow

import com.google.caliper.Param
import com.google.caliper.Runner
import com.google.caliper.SimpleBenchmark
import groovyx.gpars.actor.StaticDispatchActor
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.scheduler.FJPool

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import com.google.caliper.api.VmParam

class ThroughputBenchmarkStaticDispatchActor extends SimpleBenchmark{
    @VmParam(["-Xdebug"])String placeHolder;
    @VmParam(["-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"])String placeHolder2;
    @Param(["1", "2", "4", "6", "8",
    "10", "12", "14", "16", "18",
    "20", "22", "24", "26", "28",
    "30", "32", "34", "36", "38",
    "40", "42", "44", "46", "48"]) int numberOfClients
    public static final int RUN = 1
    public static final int MESSAGE = 2
    final int maxRunDurationMillis = 20000
    DefaultPGroup group
    long repeatsPerClient


    @Override protected void setUp() throws Exception {
        int repeatFactor = 2
        long repeat = 30000L * repeatFactor
        int maxClients = 4;
        group = new DefaultPGroup(new FJPool(maxClients))
        repeatsPerClient = repeat / numberOfClients
    }

    public void timeThroughput(int reps) {
        for (int i = 0; i < reps; i++) {
            CountDownLatch latch = new CountDownLatch(numberOfClients)
            ArrayList<DestinationActor> destinations = new ArrayList<DestinationActor>()
            ArrayList<ClientActor>clients = new ArrayList<ClientActor>()

            (1..numberOfClients).each {
                destinations.add(new DestinationActor(group).start())

            }
            destinations.each {
                clients.add(new ClientActor(it, latch, repeatsPerClient, group).start())
            }
            clients.each {
                it.send(RUN)
            }
            latch.await(maxRunDurationMillis, TimeUnit.MILLISECONDS)
            clients.each {
                it.terminate()
            }
            destinations.each {
                it.terminate();
            }
        }
    }

    static void main(String[] args) {
        new Runner().main(ThroughputBenchmarkStaticDispatchActor.class, args);
    }

}

class ClientActor extends StaticDispatchActor<Integer> {

    long sent = 0L
    long received = 0L
    DestinationActor actor
    long repeatsPerClient
    CountDownLatch latch


    public ClientActor(DestinationActor actor, CountDownLatch latch, long repeatsPerClient, DefaultPGroup group) {
        this.parallelGroup = group
        this.actor = actor
        this.repeatsPerClient = repeatsPerClient
        this.latch = latch
    }

    void onMessage(Integer msg) {
        if (msg.equals(ThroughputBenchmarkStaticDispatchActor.MESSAGE)) {
            received += 1
            if (sent < repeatsPerClient) {
                actor.send(msg)
                sent += 1
            } else if (received >= repeatsPerClient) {
                latch.countDown()
                sent=0;
                received=0;

            }
        }
        if (msg.equals(ThroughputBenchmarkStaticDispatchActor.RUN)) {
            (1..Math.min(repeatsPerClient, 1000L)).each {
                actor.send(ThroughputBenchmarkStaticDispatchActor.MESSAGE)
                sent += 1
            }
        }

    }
}

class DestinationActor extends StaticDispatchActor<Integer> {
    public DestinationActor(DefaultPGroup group) {
        this.parallelGroup = group
    }

    @Override
    void onMessage(Integer m) {
        getSender().send(ThroughputBenchmarkStaticDispatchActor.MESSAGE)
    }
}

