package groovyx.gpars.benchmark.akka;

import com.google.caliper.Param;
import com.google.caliper.api.Benchmark;
import com.google.caliper.api.VmParam;
import com.google.caliper.runner.CaliperMain;
import groovyx.gpars.actor.DynamicDispatchActor;
import groovyx.gpars.group.DefaultPGroup;
import groovyx.gpars.scheduler.FJPool;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public class BenchmarkThroughputDynamicDispatchActorCaliper extends Benchmark {
    @Param({"1", "2", "4", "6", "8",
            "10", "12", "14", "16", "18",
            "20", "22", "24", "26", "28",
            "30", "32", "34", "36", "38",
            "40", "42", "44", "46", "48"}
    )
    int numberOfClients;
    @VmParam({"-server"})
    String server;
    @VmParam({"-Xms512M"})
    String xms;
    @VmParam({"-Xmx1024M"})
    String xmx;
    @VmParam({"-XX:+UseParallelGC"})
    String gc;

    int maxClients = 4;
    public static final ThroughputDynamicRun RUN = new ThroughputDynamicRun();
    public static final ThroughputDynamicMessage MESSAGE = new ThroughputDynamicMessage();
    final int maxRunDurationMillis = 20000;
    DefaultPGroup group;
    long repeatsPerClient;
    int repeatFactor = 500;
    int repeat = 30000 * repeatFactor;

    public int totalMessages() {
        return repeat;
    }

    public long timeDynamicDispatchActorThroughput(int reps) {
        long totalTime = 0;
        group = new DefaultPGroup(new FJPool(maxClients));
        repeatsPerClient = repeat / numberOfClients;

        for (int rep = 0; rep < reps; rep++) {

            CountDownLatch latch = new CountDownLatch(numberOfClients);
            ArrayList<ThroughputDynamicClient> clients = new ArrayList<ThroughputDynamicClient>();
            ArrayList<ThroughputDynamicDestination> destinations = new ArrayList<ThroughputDynamicDestination>();

            for (int i = 0; i < numberOfClients; i++) {
                destinations.add((ThroughputDynamicDestination) new ThroughputDynamicDestination(group).start());
            }

            for (ThroughputDynamicDestination destination : destinations) {
                clients.add((ThroughputDynamicClient) new ThroughputDynamicClient(destination, latch, repeatsPerClient, group).start());
            }

            long startTime = System.nanoTime();

            for (ThroughputDynamicClient client : clients) {
                client.send(RUN);
            }
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            totalTime += System.nanoTime() - startTime;
            for (ThroughputDynamicClient client : clients) {
                client.terminate();
            }
            for (ThroughputDynamicDestination destination : destinations) {
                destination.terminate();
            }
        }
        return totalTime;
    }

    public static void main(String[] args) {
        CaliperMain.main(BenchmarkThroughputDynamicDispatchActorCaliper.class, args);
    }


}

class ThroughputDynamicMessage {
}

class ThroughputDynamicRun {
}

class ThroughputDynamicClient extends DynamicDispatchActor {

    long sent = 0L;
    long received = 0L;
    ThroughputDynamicDestination actor;
    long repeatsPerClient;
    CountDownLatch latch;

    public ThroughputDynamicClient(ThroughputDynamicDestination actor, CountDownLatch latch, long repeatsPerClient, DefaultPGroup group) {
        this.parallelGroup = group;
        this.actor = actor;
        this.repeatsPerClient = repeatsPerClient;
        this.latch = latch;
    }

    void onMessage(final ThroughputDynamicMessage msg) {
        received += 1;
        if (sent < repeatsPerClient) {
            actor.send(msg);
            sent += 1;
        } else if (received >= repeatsPerClient) {
            latch.countDown();

        }
    }

    void onMessage(final ThroughputDynamicRun msg) {
        for (int i = 0; i < Math.min(repeatsPerClient, 1000L); i++) {
            actor.send(BenchmarkThroughputDynamicDispatchActorCaliper.MESSAGE);
            sent += 1;
        }
    }
}

class ThroughputDynamicDestination extends DynamicDispatchActor {
    public ThroughputDynamicDestination(DefaultPGroup group) {
        this.parallelGroup = group;
    }

    void onMessage(final ThroughputDynamicMessage msg) {
        getSender().send(msg);
    }

}
