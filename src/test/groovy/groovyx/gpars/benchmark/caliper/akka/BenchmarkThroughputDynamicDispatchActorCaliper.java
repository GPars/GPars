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
//
// Based on a benchmark in the Akka source code

package groovyx.gpars.benchmark.caliper.akka;

import com.google.caliper.Param;
import com.google.caliper.api.VmParam;
import com.google.caliper.runner.CaliperMain;
import groovyx.gpars.actor.DynamicDispatchActor;
import groovyx.gpars.group.DefaultPGroup;

import java.util.concurrent.CountDownLatch;

public class BenchmarkThroughputDynamicDispatchActorCaliper extends BenchmarkCaliper {
    @Param({"1", "2", "4", "6", "8",
            "10", "12", "14", "16", "18",
            "20", "22", "24", "26", "28",
            "30", "32", "34", "36", "38",
            "40", "42", "44", "46", "48"}
    )
    int numberOfClients;
    @VmParam
    String server;
    @VmParam
    String xms;
    @VmParam
    String xmx;
    @VmParam
    String gc;


    BenchmarkThroughputDynamicDispatchActorCaliper() {
        super(30000, DYNAMIC_RUN, ThroughputDynamicClient.class, ThroughputDynamicDestination.class);
    }

    public long timeThroughputDynamicDispatchActor(final int reps) {
        long time = 0;
        try {
            time = timeThroughput(reps, numberOfClients);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return time;
    }

    public static void main(final String[] args) {
        CaliperMain.main(BenchmarkThroughputDynamicDispatchActorCaliper.class, args);
    }


}


class ThroughputDynamicClient extends DynamicDispatchActor {

    long sent = 0L;
    long received = 0L;
    ThroughputDynamicDestination actor;
    long repeatsPerClient;
    CountDownLatch latch;

    public ThroughputDynamicClient(final ThroughputDynamicDestination actor, final CountDownLatch latch, final long repeatsPerClient, final DefaultPGroup group) {
        this.parallelGroup = group;
        this.actor = actor;
        this.repeatsPerClient = repeatsPerClient;
        this.latch = latch;
    }

    void onMessage(final DynamicMessage msg) {
        received += 1L;
        if (sent < repeatsPerClient) {
            actor.send(msg);
            sent += 1L;
        } else if (received >= repeatsPerClient) {
            latch.countDown();
        }
    }

    void onMessage(final DynamicRun msg) {
        for (int i = 0; (long) i < Math.min(repeatsPerClient, 1000L); i++) {
            actor.send(BenchmarkCaliper.DYNAMIC_MESSAGE);
            sent += 1L;
        }
    }
}

class ThroughputDynamicDestination extends DynamicDispatchActor {

    public ThroughputDynamicDestination(final DefaultPGroup group) {
        this.parallelGroup = group;
    }

    void onMessage(final DynamicMessage msg) {
        getSender().send(msg);
    }

}
