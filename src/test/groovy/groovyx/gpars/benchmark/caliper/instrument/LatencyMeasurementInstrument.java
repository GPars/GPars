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

package groovyx.gpars.benchmark.caliper.instrument;

import com.google.caliper.api.Benchmark;
import com.google.caliper.api.SkipThisScenarioException;
import com.google.caliper.runner.BenchmarkClass;
import com.google.caliper.runner.BenchmarkMethod;
import com.google.caliper.runner.Instrument;
import com.google.caliper.runner.InvalidBenchmarkException;
import com.google.caliper.runner.UserCodeException;
import com.google.caliper.util.ShortDuration;
import com.google.caliper.util.Util;
import com.google.caliper.worker.Worker;
import com.google.common.collect.ImmutableMap;
import groovyx.gpars.benchmark.caliper.worker.LatencyMeasurementWorker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Throwables.propagateIfInstanceOf;

public class LatencyMeasurementInstrument extends Instrument {
    @Override
    public ShortDuration estimateRuntimePerTrial() {
        return ShortDuration.valueOf(options.get("maxTotalRuntime"));
    }

    @Override
    public boolean isBenchmarkMethod(final Method method) {
        return method.getName().startsWith("latency") && Util.isPublic(method);
    }

    @Override
    public BenchmarkMethod createBenchmarkMethod(final BenchmarkClass benchmarkClass, final Method method) throws InvalidBenchmarkException {
        checkArgument(isBenchmarkMethod(method));
        if (Util.isStatic(method)) {
            throw new InvalidBenchmarkException(
                    "LatencyMeasure methods must not be static: " + method.getName());
        }

        final String methodName = method.getName();
        final String shortName = methodName.substring("latency".length());
        return new BenchmarkMethod(benchmarkClass, method, shortName);
    }


    @Override
    public void dryRun(final Benchmark benchmark, final BenchmarkMethod benchmarkMethod)
            throws UserCodeException {
        final Method m = benchmarkMethod.method();
        try {
            m.invoke(benchmark, 1);
        } catch (IllegalAccessException impossible) {
            throw new AssertionError(impossible);
        } catch (InvocationTargetException e) {
            final Throwable userException = e.getCause();
            propagateIfInstanceOf(userException, SkipThisScenarioException.class);
            throw new UserCodeException(userException);
        }
    }

    @Override
    public Map<String, String> workerOptions() {
        return new ImmutableMap.Builder<String, String>()
                .put("warmupNanos", toNanosString("warmup"))
                .put("timingIntervalNanos", toNanosString("timingInterval"))
                .put("reportedIntervals", options.get("reportedIntervals"))
                .put("shortCircuitTolerance", options.get("shortCircuitTolerance"))
                .put("maxTotalRuntimeNanos", toNanosString("maxTotalRuntime"))
                .put("gcBeforeEach", options.get("gcBeforeEach"))
                .build();
    }

    @Override
    public Class<? extends Worker> workerClass() {
        return LatencyMeasurementWorker.class;
    }

    private String toNanosString(final String optionName) {
        return String.valueOf(ShortDuration.valueOf(options.get(optionName)).to(TimeUnit.NANOSECONDS));
    }

    @Override
    public boolean equals(final Object object) {
        return object instanceof LatencyMeasurementInstrument;
    }

    @Override
    public int hashCode() {
        return 0x5FE89C3A;
    }

    @Override
    public String toString() {
        return "latency";
    }
}
