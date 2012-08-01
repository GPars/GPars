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

public class LatencyMeasurementWorker implements Worker {
    @Override
    public Collection<Measurement> measure(Benchmark benchmark, String methodName, Map<String, String> optionMap, WorkerEventLog log) throws Exception {
        final int benchmarkReps = 100;
        Options options = new Options(optionMap);
        Trial trial = new Trial(benchmark, methodName, options, log);
        trial.warmUp();
        return trial.run(benchmarkReps);
    }

    private static class Trial {
        final Benchmark benchmark;
        final Method latencyMethod;
        final Options options;
        final WorkerEventLog log;
        final long startTick;
        final int repeatFactor;
        static final int warmupReps = 1;

        Trial(Benchmark benchmark, String methodName, Options options, WorkerEventLog log)
                throws Exception {
            this.benchmark = benchmark;

            // where's the right place for 'time' to be prepended again?
            this.latencyMethod = benchmark.getClass().getDeclaredMethod("latency" + methodName, int.class);
            this.options = options;
            this.log = log;
            this.startTick = System.nanoTime();
            this.repeatFactor = (Integer) benchmark.getClass().getSuperclass().getDeclaredMethod("totalMessages").invoke(benchmark);


            latencyMethod.setAccessible(true);
        }

        void warmUp() throws Exception {
            log.notifyWarmupPhaseStarting();
            invokeLatencyMethod(warmupReps);

        }

        Collection<Measurement> run(int targetReps) throws Exception {
            Queue<Measurement> measurements = new LinkedList<Measurement>();
            LastNValues recentValues = new LastNValues(options.reportedIntervals);

            log.notifyMeasurementPhaseStarting();
            for (int trials = 0; trials < 1; trials++) {
                int reps = 5;
                if (options.gcBeforeEach) {
                    Util.forceGc();
                }

                log.notifyMeasurementStarting();
                long nanos = invokeLatencyMethod(reps);

                Measurement m = new Measurement();
                m.value = nanos;
                m.weight = reps * repeatFactor;
                m.unit = "ns";
                m.description = "propagation latency";
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
            // Note the * could overflow 2^63, but only if you're being kinda insane...
            return (int) (previousReps * targetNanos / previousNanos);
        }

        private long invokeLatencyMethod(int reps) throws Exception {

            long total = 0;

            for (int i = 0; i < reps; i++) {
                total += ((Long) latencyMethod.invoke(benchmark, i));

            }
            return total;

        }
    }

    private static class Options {
        final long warmupNanos;
        final long timingIntervalNanos;
        final int reportedIntervals;
        final double shortCircuitTolerance;
        final long maxTotalRuntimeNanos;
        final boolean gcBeforeEach;

        Options(Map<String, String> optionMap) {
            this.warmupNanos = Long.parseLong(optionMap.get("warmupNanos"));
            this.timingIntervalNanos = Long.parseLong(optionMap.get("timingIntervalNanos"));
            this.reportedIntervals = Integer.parseInt(optionMap.get("reportedIntervals"));
            this.shortCircuitTolerance = Double.parseDouble(optionMap.get("shortCircuitTolerance"));
            this.maxTotalRuntimeNanos = Long.parseLong(optionMap.get("maxTotalRuntimeNanos"));
            this.gcBeforeEach = Boolean.parseBoolean(optionMap.get("gcBeforeEach"));

            if (warmupNanos + reportedIntervals * timingIntervalNanos > maxTotalRuntimeNanos) {
                throw new RuntimeException("maxTotalRuntime is too low");
            }
        }
    }

}