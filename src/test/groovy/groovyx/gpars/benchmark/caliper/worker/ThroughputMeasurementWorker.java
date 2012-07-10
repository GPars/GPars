package groovyx.gpars.benchmark.caliper.worker;

import com.google.caliper.api.Benchmark;
import com.google.caliper.model.Measurement;
import com.google.caliper.util.LastNValues;
import com.google.caliper.util.Util;
import com.google.caliper.worker.Worker;
import com.google.caliper.worker.WorkerEventLog;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class ThroughputMeasurementWorker implements Worker {
    @Override
    public Collection<Measurement> measure(Benchmark benchmark, String methodName,Map<String, String> optionMap, WorkerEventLog log) throws Exception {
        final int benchmarkReps=100;
        Options options = new Options(optionMap);
        Trial trial = new Trial(benchmark, methodName, options, log);
        trial.warmUp();
        // TODO: make the minimum configurable, default to maybe 1000?
        /*if (targetReps < 100) {
            throw new Exception("Too few reps "+targetReps); // TODO: better exception

        }       */
        return trial.run(benchmarkReps);
    }

    private static class Trial {
        final Benchmark benchmark;
        final Method timeMethod;
        final Options options;
        final WorkerEventLog log;
        final long startTick;
        final int totalMessages;
        final int warmupReps=1;

        Trial(Benchmark benchmark, String methodName, Options options, WorkerEventLog log)
                throws Exception {
            this.benchmark = benchmark;

            // where's the right place for 'time' to be prepended again?
            this.timeMethod = benchmark.getClass().getDeclaredMethod("time" + methodName, int.class);
            this.options = options;
            this.log = log;
            this.startTick = System.nanoTime(); // TODO: Ticker?
            this.totalMessages = (Integer) benchmark.getClass().getDeclaredMethod("totalMessages", null).invoke(benchmark);
            timeMethod.setAccessible(true);
        }

        void warmUp() throws Exception {
            log.notifyWarmupPhaseStarting();
            invokeTimeMethod(warmupReps);
        }

        Collection<Measurement> run(int targetReps) throws Exception {
            // Use a variety of rep counts for measurements, so that things like concavity and
            // y-intercept might be observed later. Just cycle through them in order (why not?).
            int[] repCounts = {
                    // (int) (targetReps * 0.7),
                    // (int) (targetReps * 0.9),
                    (int) (targetReps * 1.1),
                    (int) (targetReps * 1.3)
            };

            long timeToStop = startTick + options.maxTotalRuntimeNanos - options.timingIntervalNanos;

            Queue<Measurement> measurements = new LinkedList<Measurement>();

            LastNValues recentValues = new LastNValues(options.reportedIntervals);
            int i = 0;

            log.notifyMeasurementPhaseStarting();

            //while (System.nanoTime() < timeToStop) {
            for(int trials=0;trials<1;trials++){
                //int reps = repCounts[i++ % repCounts.length];
                int reps=5;
                if (options.gcBeforeEach) {
                    Util.forceGc();
                }
                log.notifyMeasurementStarting();
                long nanos = invokeTimeMethod(reps);
                double nanos_per_rep = nanos / reps;
                double seconds = (double) nanos_per_rep / 1000000000;
                double messages_per_second = totalMessages / seconds;

                Measurement m = new Measurement();
                m.value = messages_per_second;
                m.weight = 1;
                m.unit = "Messages per Second";
                m.description = "";
                double nanosPerRep = m.value / m.weight;
                log.notifyMeasurementEnding(nanosPerRep);

                measurements.add(m);
                if (measurements.size() > options.reportedIntervals) {
                    measurements.remove();
                }
                recentValues.add(nanosPerRep);

                if (shouldShortCircuit(recentValues)) {
                    break;
                }
            }

            return measurements;
        }

        private boolean shouldShortCircuit(LastNValues lastN) {
            return lastN.isFull() && lastN.normalizedStddev() < options.shortCircuitTolerance;
        }

        private static int adjustRepCount(int previousReps, long previousNanos, long targetNanos) {
            // TODO: friendly error on overflow?
            // Note the * could overflow 2^63, but only if you're being kinda insane...
            return (int) (previousReps * targetNanos / previousNanos);
        }

        private long invokeTimeMethod(int reps) throws Exception {
            Object temp = timeMethod.invoke(benchmark, reps);
            Long tempTime = (Long) temp;
            long time = tempTime.longValue();
            return time;
        }
    }

    private static class Options {
        long warmupNanos;
        long timingIntervalNanos;
        int reportedIntervals;
        double shortCircuitTolerance;
        long maxTotalRuntimeNanos;
        boolean gcBeforeEach;

        Options(Map<String, String> optionMap) {
            this.warmupNanos = Long.parseLong(optionMap.get("warmupNanos"));
            this.timingIntervalNanos = Long.parseLong(optionMap.get("timingIntervalNanos"));
            this.reportedIntervals = Integer.parseInt(optionMap.get("reportedIntervals"));
            this.shortCircuitTolerance = Double.parseDouble(optionMap.get("shortCircuitTolerance"));
            this.maxTotalRuntimeNanos = Long.parseLong(optionMap.get("maxTotalRuntimeNanos"));
            this.gcBeforeEach = Boolean.parseBoolean(optionMap.get("gcBeforeEach"));

            if (warmupNanos + reportedIntervals * timingIntervalNanos > maxTotalRuntimeNanos) {
                throw new RuntimeException("maxTotalRuntime is too low"); // TODO
            }
        }
    }


}

