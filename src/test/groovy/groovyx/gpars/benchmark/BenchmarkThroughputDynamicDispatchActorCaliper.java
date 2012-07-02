package groovyx.gpars.benchmark;

import com.google.caliper.Param;
import com.google.caliper.api.Benchmark;
import com.google.caliper.api.VmParam;
import com.google.caliper.runner.CaliperMain;
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
    @VmParam({"-server"}) String server;
    @VmParam({"-Xms512M"}) String xms;
    @VmParam({"-Xmx1024M"}) String xmx;
    @VmParam({"-XX:+UseParallelGC"}) String gc;

    int maxClients = 4;
    public static final Run RUN =new Run();
    public static final Message MESSAGE = new Message();
    final int maxRunDurationMillis = 20000;
    DefaultPGroup group;
    long repeatsPerClient;
    int repeatFactor = 500;
    int repeat = 30000 * repeatFactor;

    public int totalMessages(){
        return repeat;
    }

    public long timeDynamicDispatchActorThroughput(int reps) {
        long totalTime=0;
        group = new DefaultPGroup(new FJPool(maxClients));
        repeatsPerClient = repeat / numberOfClients;

        for (int rep = 0; rep< reps; rep++) {

            CountDownLatch latch = new CountDownLatch(numberOfClients);
            ArrayList<Client> clients = new ArrayList<Client>();
            ArrayList<Destination> destinations = new ArrayList<Destination>();

            for (int i = 0; i < numberOfClients; i++) {
                destinations.add((Destination)new Destination(group).start());
            }

            for (Destination destination : destinations) {
                clients.add((Client)new Client(destination, latch, repeatsPerClient, group).start());
            }

            long startTime = System.nanoTime();

            for (Client client : clients) {
                client.send(RUN);
            }
            try {
                latch.await(maxRunDurationMillis, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            totalTime+=System.nanoTime()-startTime;
            for (Client client : clients) {
                client.terminate();
            }
            for (Destination destination : destinations) {
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
    Destination actor;
    long repeatsPerClient;
    CountDownLatch latch;

    public Client(Destination actor, CountDownLatch latch, long repeatsPerClient, DefaultPGroup group) {
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
                actor.send(BenchmarkThroughputDynamicDispatchActorCaliper.MESSAGE);
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
