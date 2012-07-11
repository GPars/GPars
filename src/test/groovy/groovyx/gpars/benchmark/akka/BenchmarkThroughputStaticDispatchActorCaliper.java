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
import com.google.caliper.api.VmParam;
import com.google.caliper.runner.CaliperMain;
import groovyx.gpars.actor.StaticDispatchActor;
import groovyx.gpars.group.DefaultPGroup;

import java.util.concurrent.CountDownLatch;

public class BenchmarkThroughputStaticDispatchActorCaliper extends BenchmarkCaliper {

    @Param({"1", "2", "4", "6", "8",
            "10", "12", "14", "16", "18",
            "20", "22", "24", "26", "28",
            "30", "32", "34", "36", "38",
            "40", "42", "44", "46", "48"}
    )
    int numberOfClients;

    @VmParam String server;
    @VmParam String xms;
    @VmParam String xmx;
    @VmParam String gc;


    BenchmarkThroughputStaticDispatchActorCaliper(){
       super(30000,BenchmarkCaliper.STATIC_RUN, ThroughputStaticClient.class,ThroughputStaticDestination.class);
    }

    public long timeStaticDispatchActorThroughput(int reps) {
        long time =0;
        try {
          time = super.timeThroughput(reps, numberOfClients);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return time;
    }

    public static void main(String[] args) {
        CaliperMain.main(BenchmarkThroughputStaticDispatchActorCaliper.class, args);
    }

}

class ThroughputStaticClient extends StaticDispatchActor<Integer> {

    long sent = 0L;
    long received = 0L;
    ThroughputStaticDestination actor;
    long repeatsPerClient;
    CountDownLatch latch;


    public ThroughputStaticClient(ThroughputStaticDestination actor, CountDownLatch latch, long repeatsPerClient, DefaultPGroup group) {
        this.parallelGroup = group;
        this.actor = actor;
        this.repeatsPerClient = repeatsPerClient;
        this.latch = latch;
    }

    @Override
    public void onMessage(Integer msg) {
        if (msg.equals(BenchmarkCaliper.STATIC_MESSAGE)) {
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
        if (msg.equals(BenchmarkCaliper.STATIC_RUN)) {
            for (int i = 0; i < (Math.min(repeatsPerClient, 1000L)); i++) {
                actor.send(BenchmarkCaliper.STATIC_MESSAGE);
                sent += 1;
            }
        }
    }
}

class ThroughputStaticDestination extends StaticDispatchActor<Integer> {
    public ThroughputStaticDestination(DefaultPGroup group) {
        this.parallelGroup = group;
    }

    @Override
    public void onMessage(Integer msg) {
        getSender().send(msg);
    }
}
