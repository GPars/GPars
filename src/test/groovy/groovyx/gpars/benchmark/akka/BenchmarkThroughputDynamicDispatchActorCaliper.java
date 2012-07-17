package groovyx.gpars.benchmark.akka;

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
    @VmParam String server;
    @VmParam String xms;
    @VmParam String xmx;
    @VmParam String gc;


    BenchmarkThroughputDynamicDispatchActorCaliper(){
        super(30000, DYNAMIC_RUN, ThroughputDynamicClient.class, ThroughputDynamicDestination.class);
    }

    public long timeThroughputDynamicDispatchActor(int reps) {
        long time=0;
        try {
            time = super.timeThroughput(reps, numberOfClients);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return time;
    }

    public static void main(String[] args) {
        CaliperMain.main(BenchmarkThroughputDynamicDispatchActorCaliper.class, args);
    }


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

    void onMessage(final DynamicMessage msg) {
        received += 1;
        if (sent < repeatsPerClient) {
            actor.send(msg);
            sent += 1;
        } else if (received >= repeatsPerClient) {
            latch.countDown();
        }
    }

    void onMessage(final DynamicRun msg) {
        for (int i = 0; i < Math.min(repeatsPerClient, 1000L); i++) {
            actor.send(BenchmarkCaliper.DYNAMIC_MESSAGE);
            sent += 1;
        }
    }
}

class ThroughputDynamicDestination extends DynamicDispatchActor {

    public ThroughputDynamicDestination(DefaultPGroup group) {
        this.parallelGroup = group;
    }

    void onMessage(final DynamicMessage msg) {
        getSender().send(msg);
    }

}
