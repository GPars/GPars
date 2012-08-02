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

package groovyx.gpars.benchmark.caliper.akka;

import com.google.caliper.api.Benchmark;
import groovyx.gpars.actor.Actor;
import groovyx.gpars.group.DefaultPGroup;
import groovyx.gpars.scheduler.FJPool;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

public abstract class BenchmarkCaliper extends Benchmark {
    public static final int STATIC_RUN = 1;
    public static final int STATIC_MESSAGE = 2;
    public static final int STATIC_POISON = 3;
    public static final DynamicRun DYNAMIC_RUN = new DynamicRun();
    public static final DynamicMessage DYNAMIC_MESSAGE = new DynamicMessage();
    public static final Poison DYNAMIC_POISON = new Poison();

    final Object RUN;
    final Object POISON;
    public int maxClients = 4;
    int repeatFactor = 500;
    int totalPerRep;
    int repeat;
    int repeatsPerClient;
    private long totalDuration;
    DefaultPGroup group;
    Class<? extends Actor> clientType, destinationType, waypointType;
    Collection<Actor> destinations;
    Collection<Actor> clients;
    CountDownLatch latch;

    BenchmarkCaliper() {
        // shouldn't be called
        RUN = null;
        POISON = null;
    }

    BenchmarkCaliper(int totalPerRep, Object run, Class clientType, Class destinationType) {
        this.totalPerRep = totalPerRep;
        this.repeat = totalPerRep * repeatFactor;
        this.RUN = run;
        this.POISON = null;
        this.clientType = clientType;
        this.destinationType = destinationType;
    }

    BenchmarkCaliper(final int totalPerRep, final Object run, final Object poison, final Class clientType, final Class destinationType, final Class waypointType) {
        this.totalPerRep = totalPerRep;
        this.repeat = totalPerRep * repeatFactor;
        this.RUN = run;
        this.POISON = poison;
        this.clientType = clientType;
        this.destinationType = destinationType;
        this.waypointType = waypointType;
    }

    public int totalMessages() {
        return repeat;
    }

    public long timeThroughput(final int reps, final int numberOfClients) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException, InterruptedException {
        group = new DefaultPGroup(new FJPool(maxClients));
        repeatsPerClient = repeat / numberOfClients;//MESSAGE quota for each pair of actors

        long totalTime = 0L;
        for (int i = 0; i < reps; i++) {

            latch = new CountDownLatch(numberOfClients);
            destinations = new ArrayList<Actor>();
            clients = new ArrayList<Actor>();

            for (int j = 0; j < numberOfClients; j++) {
                destinations.add(destinationType.getConstructor(new Class<?>[]{DefaultPGroup.class}).newInstance(group).start());

            }
            for (final Actor destination : destinations) {
                clients.add(clientType.getConstructor(new Class<?>[]{destinationType, CountDownLatch.class, long.class, DefaultPGroup.class}).newInstance(destination, latch, repeatsPerClient, group).start());
            }

            final long startTime = System.nanoTime();//start timing

            for (final Actor client : clients) {
                client.send(RUN);
            }

            latch.await();


            totalTime += System.nanoTime() - startTime;//stop timing

            for (final Actor client : clients) {
                client.terminate();
            }
            for (final Actor destination : destinations) {
                destination.terminate();
            }
        }
        return totalTime;

    }

    public long timeLatency(final int numberOfClients) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, InterruptedException {
        setupLatencyBenchmark(numberOfClients);

        for (final Actor client : clients) {
            client.start();
            client.send(RUN);
        }

        latch.await();
        teardownLatencyBenchmark();
        synchronized (this) {
            return totalDuration;
        }
    }

    private void setupLatencyBenchmark(final int numberOfClients) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {

        totalDuration = 0L;
        group = new DefaultPGroup(new FJPool(maxClients));
        latch = new CountDownLatch(numberOfClients);
        repeatsPerClient = repeat / numberOfClients;
        clients = new ArrayList<Actor>();

        for (int i = 0; i < numberOfClients; i++) {
            final Actor destination = destinationType.getConstructor(new Class<?>[]{DefaultPGroup.class}).newInstance(group).start();
            final Actor w4 = waypointType.getConstructor(new Class<?>[]{Actor.class, DefaultPGroup.class}).newInstance(destination, group).start();
            final Actor w3 = waypointType.getConstructor(new Class<?>[]{Actor.class, DefaultPGroup.class}).newInstance(w4, group).start();
            final Actor w2 = waypointType.getConstructor(new Class<?>[]{Actor.class, DefaultPGroup.class}).newInstance(w3, group).start();
            final Actor w1 = waypointType.getConstructor(new Class<?>[]{Actor.class, DefaultPGroup.class}).newInstance(w2, group).start();
            clients.add(clientType.getConstructor(new Class<?>[]{Actor.class, CountDownLatch.class, long.class, DefaultPGroup.class, BenchmarkCaliper.class}).newInstance(w1, latch, repeatsPerClient, group, this));
        }
    }

    private void teardownLatencyBenchmark() throws InterruptedException {
        for (final Actor client : clients) {
            client.send(POISON);
        }

        for (final Actor client : clients) {
            client.join();
        }
        group.shutdown();
    }

    public synchronized void addDuration(final long duration) {
        totalDuration += duration;
    }
}

class DynamicRun {
}

class DynamicMessage {
}

class Poison {
}

class LatencyMessage {
    final long sendTime;
    final Actor sender;
    int msg;

    LatencyMessage(final long sendTime, final Actor sender, final int msg) {
        this.sendTime = sendTime;
        this.sender = sender;
        this.msg = msg;
    }

    LatencyMessage(final long sendTime, final Actor sender) {
        this.sendTime = sendTime;
        this.sender = sender;
    }

    public Actor sender() {
        return sender;
    }
}
