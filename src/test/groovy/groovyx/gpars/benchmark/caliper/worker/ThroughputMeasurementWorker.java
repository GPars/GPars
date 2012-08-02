// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
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
    public Collection<Measurement> measure(Benchmark benchmark, String methodName, Map<String, String> optionMap, WorkerEventLog log) throws Exception {
        final int benchmarkReps = 100;
        Options options = new Options(optionMap);
        Trial trial = new Trial(benchmark, methodName, options, log);
        trial.warmUp();
        return trial.run(benchmarkReps);
    }

    private static class Trial {
        final Benchmark benchmark;
        final Method timeMethod;
        final Options options;
        final WorkerEventLog log;
        final long startTick;
        final int totalMessages;
        static final int warmupReps = 1;

        Trial(Benchmark benchmark, String methodName, Options options, WorkerEventLog log)
                throws Exception {
            this.benchmark = benchmark;

            // where's the right place for 'time' to be prepended again?
            this.timeMethod = benchmark.getClass().getDeclaredMethod("time" + methodName, int.class);
            this.options = options;
            this.log = log;
            this.startTick = System.nanoTime();
            this.totalMessages = (Integer) benchmark.getClass().getSuperclass().getDeclaredMethod("totalMessages").invoke(benchmark);
            timeMethod.setAccessible(true);
        }

        void warmUp() throws Exception {
            log.notifyWarmupPhaseStarting();
            invokeTimeMethod(warmupReps);
        }

        Collection<Measurement> run(int targetReps) throws Exception {
            long timeToStop = startTick + options.maxTotalRuntimeNanos - options.timingIntervalNanos;
            Queue<Measurement> measurements = new LinkedList<Measurement>();
            LastNValues recentValues = new LastNValues(options.reportedIntervals);

            log.notifyMeasurementPhaseStarting();

            for (int trials = 0; trials < 1; trials++) {
                int reps = 5;
                if (options.gcBeforeEach) Util.forceGc();
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

