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
import groovyx.gpars.actor.StaticDispatchActor;
import groovyx.gpars.group.DefaultPGroup;

import java.util.Random;
import java.util.concurrent.CountDownLatch;


public class BenchmarkLatencyStaticDispatchActorCaliper extends BenchmarkCaliper {
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

    BenchmarkLatencyStaticDispatchActorCaliper() {
        super(200, new LatencyMessage(0L, null, STATIC_RUN), new LatencyMessage(0L, null, STATIC_POISON), LatencyStaticClient.class, LatencyStaticDestination.class, LatencyStaticWayPoint.class);
    }

    public long latencyLatencyStaticDispatchActor(final int dummy) {
        long time = 0;
        try {
            time = timeLatency(numberOfClients);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return time;
    }

    public static void main(final String[] args) {
        CaliperMain.main(BenchmarkLatencyStaticDispatchActorCaliper.class, args);
    }
}

class LatencyStaticWayPoint extends StaticDispatchActor<LatencyMessage> {
    final Actor next;

    public LatencyStaticWayPoint(final Actor next, final DefaultPGroup group) {
        this.next = next;
        this.parallelGroup = group;
        // this.makeFair();
    }

    @Override
    public void onMessage(final LatencyMessage msg) {
        if (msg.msg == BenchmarkCaliper.STATIC_MESSAGE) {
            next.send(msg);
        } else if (msg.msg == BenchmarkCaliper.STATIC_POISON) {
            next.send(msg);
            this.terminate();
        }
    }

}

class LatencyStaticDestination extends StaticDispatchActor<LatencyMessage> {

    public LatencyStaticDestination(final DefaultPGroup group) {
        //this.makeFair();
        this.parallelGroup = group;
    }

    @Override
    public void onMessage(final LatencyMessage msg) {
        if (msg.msg == BenchmarkCaliper.STATIC_MESSAGE) {
            msg.sender().send(msg);
        } else if (msg.msg == BenchmarkCaliper.STATIC_POISON) {
            this.terminate();
        }
    }

}

class LatencyStaticClient extends StaticDispatchActor<LatencyMessage> {
    long sent = 0L;
    long received = 0L;
    final Actor next;
    CountDownLatch latch;
    final long repeat;
    final BenchmarkCaliper benchmark;

    public LatencyStaticClient(final Actor next, final CountDownLatch latch, final long repeat, final DefaultPGroup group, final BenchmarkCaliper benchmark) {
        this.next = next;
        this.latch = latch;
        this.repeat = repeat;
        this.parallelGroup = group;
        this.benchmark = benchmark;
        //this.makeFair();
    }

    static void shortDelay(final int micros, final long n) {
        if (micros > 0) {
            final int sampling = 1000 / micros;
            if (n % (long) sampling == 0L) {
                try {
                    Thread.sleep(1L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onMessage(final LatencyMessage msg) {
        if (msg.msg == BenchmarkCaliper.STATIC_MESSAGE) {
            final long duration = System.nanoTime() - msg.sendTime;
            benchmark.addDuration(duration);
            received++;
            if (sent < repeat) {
                shortDelay(250, received);  // Value used by Akka
                next.send(new LatencyMessage(System.nanoTime(), this, BenchmarkCaliper.STATIC_MESSAGE));
                sent++;
            } else if (received >= repeat) {
                latch.countDown();
            }
        } else if (msg.msg == BenchmarkCaliper.STATIC_RUN) {
            final int initialDelay = new Random(0).nextInt(20);    //Value used by Akka
            try {
                Thread.sleep((long) initialDelay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            next.send(new LatencyMessage(System.nanoTime(), this, BenchmarkCaliper.STATIC_MESSAGE));
            sent++;
        } else if (msg.msg == BenchmarkCaliper.STATIC_POISON) {
            next.send(msg);
            terminate();
        }
    }

}
