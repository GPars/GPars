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
    @Override public ShortDuration estimateRuntimePerTrial() {
        return ShortDuration.valueOf(options.get("maxTotalRuntime"));
    }

    @Override public boolean isBenchmarkMethod(Method method) {
        return method.getName().startsWith("latency") && Util.isPublic(method);
    }

    @Override public BenchmarkMethod createBenchmarkMethod(BenchmarkClass benchmarkClass,
                                                           Method method) throws InvalidBenchmarkException {

        checkArgument(isBenchmarkMethod(method));


        // Static technically doesn't hurt anything, but it's just the completely wrong idea
        if (Util.isStatic(method)) {
            throw new InvalidBenchmarkException(
                    "LatencyMeasure methods must not be static: " + method.getName());
        }

        String methodName = method.getName();
        String shortName = methodName.substring("latency".length());
        return new BenchmarkMethod(benchmarkClass, method, shortName);
    }



    @Override public void dryRun(Benchmark benchmark, BenchmarkMethod benchmarkMethod)
            throws UserCodeException {
        Method m = benchmarkMethod.method();
        try {
            m.invoke(benchmark,1);
        } catch (IllegalAccessException impossible) {
            throw new AssertionError(impossible);
        } catch (InvocationTargetException e) {
            Throwable userException = e.getCause();
            propagateIfInstanceOf(userException, SkipThisScenarioException.class);
            throw new UserCodeException(userException);
        }
    }

    @Override public Map<String, String> workerOptions() {
        return new ImmutableMap.Builder<String, String>()
                .put("warmupNanos", toNanosString("warmup"))
                .put("timingIntervalNanos", toNanosString("timingInterval"))
                .put("reportedIntervals", options.get("reportedIntervals"))
                .put("shortCircuitTolerance", options.get("shortCircuitTolerance"))
                .put("maxTotalRuntimeNanos", toNanosString("maxTotalRuntime"))
                .put("gcBeforeEach", options.get("gcBeforeEach"))
                .build();
    }

    @Override public Class<? extends Worker> workerClass() {
        return LatencyMeasurementWorker.class;
    }

    private String toNanosString(String optionName) {
        return String.valueOf(ShortDuration.valueOf(options.get(optionName)).to(TimeUnit.NANOSECONDS));
    }

    @Override public boolean equals(Object object) {
        return object instanceof LatencyMeasurementInstrument; // currently this class is stateless.
    }

    @Override public int hashCode() {
        return 0x5FE89C3A;
    }

    @Override public String toString() {
        return "latency";
    }
}
