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
import com.google.caliper.api.VmParam

class ThroughputBenchmark {


    int repeatFactor = 2
    long repeat = 30000L * repeatFactor
    int maxClients = 4;      int numberOfClients = 8;
    DefaultPGroup group = new DefaultPGroup(new FJPool(maxClients))
    Run run = new Run()
    public static final Message message = new Message()


    final int maxRunDurationMillis = 20000

    public void timeThroughput() {
        CountDownLatch latch = new CountDownLatch(numberOfClients)
        long repeatsPerClient = repeat / numberOfClients
        ArrayList<Client> clients = new ArrayList<Client>()     //put lists inside method
        ArrayList<Destination> destinations = new ArrayList<Destination>()

        (1..numberOfClients).each {
            destinations.add(new Destination(group).start())
        }
        destinations.each {destination ->
            clients.add(new Client(destination, latch, repeatsPerClient, group).start())
        }
        long start = System.nanoTime()
        clients.each {
            it.send(run)
        }

        latch.await(maxRunDurationMillis, TimeUnit.MILLISECONDS)
        long durationNs = (System.nanoTime() - start)
        clients.each {
            it.terminate()
        }
        destinations.each {
            it.terminate();
        }
        println durationNs / 1000000 + " Dynamic Dispatch With Instance";
    }




    static void main(String[] args) {
        ThroughputBenchmark tb = new ThroughputBenchmark()
        for (int i = 0; i < 40;i++)
            tb.timeThroughput()
    }

}
private class Message {}
class Run {}
private class Client extends DynamicDispatchActor {

    long sent = 0L
    long received = 0L
    Destination actor
    long repeatsPerClient
    CountDownLatch latch


    public Client(Destination actor, CountDownLatch latch, long repeatsPerClient, DefaultPGroup group) {
        this.parallelGroup = group
        this.actor = actor
        this.repeatsPerClient = repeatsPerClient
        this.latch = latch
    }



    void onMessage(final Object msg) {
        if (msg instanceof Message) {
            received += 1
            if (sent < repeatsPerClient) {
                actor.send(msg)
                sent += 1
            } else if (received >= repeatsPerClient) {
                latch.countDown()

            }
        }
        if (msg instanceof Run) {
            (1..Math.min(repeatsPerClient, 1000L)).each {
                actor.send(ThroughputBenchmark.message)
                sent += 1
            }
        }
    }
}

 private class Destination extends DynamicDispatchActor {
    public Destination(DefaultPGroup group) {
        this.parallelGroup = group
    }

    void onMessage(final Object msg) {
        getSender().send(msg)
    }

}

