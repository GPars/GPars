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

package groovyx.gpars.benchmark.akka;

import com.google.caliper.Param;
import com.google.caliper.api.Benchmark;
import com.google.caliper.runner.CaliperMain;
import groovyx.gpars.actor.StaticDispatchActor;
import groovyx.gpars.group.DefaultPGroup;
import groovyx.gpars.scheduler.FJPool;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: don
 * Date: 6/28/12
 * Time: 3:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class BenchmarkThroughputStaticDispatchActorCaliper extends Benchmark {

    @Param({"1", "2", "4", "6", "8",
            "10", "12", "14", "16", "18",
            "20", "22", "24", "26", "28",
            "30", "32", "34", "36", "38",
            "40", "42", "44", "46", "48"}) int numberOfClients;
    public static final int RUN = 1;
    public static final int MESSAGE = 2;
    final int maxRunDurationMillis = 20000;
    DefaultPGroup group;
    long repeatsPerClient;


    public long timeThroughput(int reps) {
        long totalTime =0;
        for (int i = 0; i < reps; i++) {
            int repeatFactor = 2;
            long repeat = 30000L * repeatFactor;
            int maxClients = 4;
            group = new DefaultPGroup(new FJPool(maxClients));
            repeatsPerClient = repeat / numberOfClients;
            CountDownLatch latch = new CountDownLatch(numberOfClients);
            ArrayList<DestinationActor> destinations = new ArrayList<DestinationActor>();
            ArrayList<ClientActor> clients = new ArrayList<ClientActor>();

            long startTime = System.nanoTime();
            for(int j=0; j < numberOfClients; j++){
                destinations.add((DestinationActor)new DestinationActor(group).start());

            }
            for(DestinationActor actor: destinations){
                clients.add((ClientActor)new ClientActor(actor, latch, repeatsPerClient, group).start());
            }
            for(ClientActor client: clients) {
                client.send(RUN);
            }
            try {
                latch.await(maxRunDurationMillis, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            totalTime += System.nanoTime()- startTime;

            for(ClientActor client: clients){
                client.terminate();
            }
            for(DestinationActor destination: destinations) {
                destination.terminate();
            }
        }
        return totalTime;
    }

    static void main(String[] args) {
        CaliperMain.main(BenchmarkThroughputStaticDispatchActorCaliper.class, args);
    }

}

class ClientActor extends StaticDispatchActor<Integer> {

    long sent = 0L;
    long received = 0L;
    DestinationActor actor;
    long repeatsPerClient;
    CountDownLatch latch;


    public ClientActor(DestinationActor actor, CountDownLatch latch, long repeatsPerClient, DefaultPGroup group) {
        this.parallelGroup = group;
        this.actor = actor;
        this.repeatsPerClient = repeatsPerClient;
        this.latch = latch;
    }

    @Override
    public void onMessage(Integer msg) {
        if (msg.equals(BenchmarkThroughputStaticDispatchActorCaliper.MESSAGE)) {
            received += 1;
            if (sent < repeatsPerClient) {
                actor.send(msg);
                sent += 1;
            } else if (received >= repeatsPerClient) {
                latch.countDown();
                sent = 0;
                received = 0;

            }
        }
        if (msg.equals(BenchmarkThroughputStaticDispatchActorCaliper.RUN)) {
            for(int i=0; i < (Math.min(repeatsPerClient, 1000L)); i++) {
                actor.send(BenchmarkThroughputStaticDispatchActorCaliper.MESSAGE);
                sent += 1;
            }
        }

    }
}

class DestinationActor extends StaticDispatchActor<Integer> {
    public DestinationActor(DefaultPGroup group) {
        this.parallelGroup = group;
    }

    @Override
    public void onMessage(Integer m) {
        getSender().send(BenchmarkThroughputStaticDispatchActorCaliper.MESSAGE);
    }
}
