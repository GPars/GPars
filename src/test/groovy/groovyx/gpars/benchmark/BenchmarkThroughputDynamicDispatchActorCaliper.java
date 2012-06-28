package groovyx.gpars.benchmark;

import com.google.caliper.Param;
import com.google.caliper.api.Benchmark;
import com.google.caliper.runner.CaliperMain;
import groovyx.gpars.actor.Actor;
import groovyx.gpars.actor.DynamicDispatchActor;
import groovyx.gpars.group.DefaultPGroup;
import groovyx.gpars.scheduler.FJPool;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class BenchmarkThroughputDynamicDispatchActorCaliper extends Benchmark {
    @Param({"1", "2", "4", "6", "8",
            "10", "12", "14", "16", "18",
            "20", "22", "24", "26", "28",
            "30", "32", "34", "36", "38",
            "40", "42", "44", "46", "48"}
    ) int numberOfClients;
    int repeatFactor = 2;
    long repeat = 30000L * repeatFactor;
    int maxClients = 4;
    DefaultPGroup group = new DefaultPGroup(new FJPool(maxClients));
    Run run = new Run();
    public static final Message message = new Message();
    final int maxRunDurationMillis = 20000;

    public long timeThroughput(int reps) {
        long totalTime=0;
        for (int rep = 0; rep< reps; rep++) {
            CountDownLatch latch = new CountDownLatch(numberOfClients);
            long repeatsPerClient = repeat / numberOfClients;
            ArrayList<Actor> clients = new ArrayList<Actor>();     //put lists inside method
            ArrayList<Actor> destinations = new ArrayList<Actor>();
            for (int i = 0; i < numberOfClients; i++) {
                destinations.add(new Destination(group).start());
            }

            for (Actor destination : destinations) {
                clients.add(new Client(destination, latch, repeatsPerClient, group).start());
            }

            long startTime = System.nanoTime();
            Run run = new Run();
            for (Actor client : clients) {
                client.send(run);
            }
            try {
                latch.await(maxRunDurationMillis, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            totalTime+=System.nanoTime()-startTime;
            for (Actor client : clients) {
                client.terminate();
            }
            for (Actor destination : destinations) {
                destination.terminate();
            }
        }
        return totalTime;
    }

    public static void main(String[] args) {
        CaliperMain.main(BenchmarkThroughputDynamicDispatchActorCaliper.class, args);
    }


}

class Message {}

class Run {}

class Client extends DynamicDispatchActor {

    long sent = 0L;
    long received = 0L;
    Actor actor;
    long repeatsPerClient;
    CountDownLatch latch;

    public Client(Actor actor, CountDownLatch latch, long repeatsPerClient, DefaultPGroup group) {
        this.parallelGroup = group;
        this.actor = actor;
        this.repeatsPerClient = repeatsPerClient;
        this.latch = latch;
    }

    void onMessage(final Object msg) {
        if (msg instanceof Message) {
            received += 1;
            if (sent < repeatsPerClient) {
                actor.send(msg);
                sent += 1;
            } else if (received >= repeatsPerClient) {
                latch.countDown();

            }
        }
        if (msg instanceof Run) {
            for (int i = 0; i < Math.min(repeatsPerClient, 1000L); i++) {
                actor.send(BenchmarkThroughputDynamicDispatchActorCaliper.message);
                sent += 1;
            }
        }
    }
}

class Destination extends DynamicDispatchActor {
    public Destination(DefaultPGroup group) {
        this.parallelGroup = group;
    }

    void onMessage(final Object msg) {
        getSender().send(msg);
    }

}
