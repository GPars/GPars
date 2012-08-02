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
import groovyx.gpars.actor.Actor;
import groovyx.gpars.actor.DynamicDispatchActor;
import groovyx.gpars.group.DefaultPGroup;

import java.util.Random;
import java.util.concurrent.CountDownLatch;


public class BenchmarkLatencyDynamicDispatchActorCaliper extends BenchmarkCaliper {

    @Param({"1", "2", "4"})
    int numberOfClients;

    @VmParam
    String server;
    @VmParam
    String xms;
    @VmParam
    String xmx;
    @VmParam
    String gc;

    BenchmarkLatencyDynamicDispatchActorCaliper() {
        super(200, DYNAMIC_RUN, DYNAMIC_POISON, LatencyDynamicClient.class, LatencyDynamicDestination.class, LatencyDynamicWayPoint.class);
    }

    public long latencyLatencyDynamicDispatchActor(final int dummy) {
        long time = 0;
        try {
            time = timeLatency(numberOfClients);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return time;
    }

    public static void main(final String[] args) {
        CaliperMain.main(BenchmarkLatencyDynamicDispatchActorCaliper.class, args);
    }
}


class LatencyDynamicWayPoint extends DynamicDispatchActor {
    final Actor next;

    public LatencyDynamicWayPoint(final Actor next, final DefaultPGroup group) {
        this.next = next;
        this.parallelGroup = group;
        //this.makeFair();
    }

    public void onMessage(final LatencyMessage msg) {
        next.send(msg);
    }

    public void onMessage(final Poison msg) {
        next.send(msg);
        terminate();
    }

}

class LatencyDynamicDestination extends DynamicDispatchActor {

    public LatencyDynamicDestination(final DefaultPGroup group) {
        this.parallelGroup = group;
        //this.makeFair();
    }

    public void onMessage(final LatencyMessage msg) {
        msg.sender().send(msg);
    }

    public void onMessage(final Poison msg) {
        terminate();
    }

}

class LatencyDynamicClient extends DynamicDispatchActor {
    long sent = 0L;
    long received = 0L;
    final Actor next;
    CountDownLatch latch;
    final long repeat;
    final BenchmarkCaliper benchmark;

    public LatencyDynamicClient(final Actor next, final CountDownLatch latch, final long repeat, final DefaultPGroup group, final BenchmarkCaliper benchmark) {
        this.next = next;
        this.latch = latch;
        this.repeat = repeat;
        this.parallelGroup = group;
        this.benchmark = benchmark;
        //this.makeFair();
    }

    void shortDelay(final int micros, final long n) {
        if (micros > 0) {
            final int sampling = 1000 / micros;
            if (n % sampling == 0) {
                try {
                    Thread.sleep(1L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void onMessage(final LatencyMessage msg) {

        final long duration = System.nanoTime() - msg.sendTime;
        benchmark.addDuration(duration);
        received++;
        if (sent < repeat) {
            shortDelay(250, received);  // value used by Akka
            next.send(new LatencyMessage(System.nanoTime(), this));
            sent++;
        } else if (received >= repeat) {
            latch.countDown();
        }

    }

    public void onMessage(final DynamicRun msg) {
        final int initialDelay = new Random(0).nextInt(20);   // Value used by Akka
        try {
            Thread.sleep((long) initialDelay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        next.send(new LatencyMessage(System.nanoTime(), this));
        sent++;
    }

    public void onMessage(final Poison msg) {
        next.send(msg);
        terminate();
    }
}
