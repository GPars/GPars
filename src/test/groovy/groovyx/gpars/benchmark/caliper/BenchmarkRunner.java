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


package groovyx.gpars.benchmark.caliper;

import com.google.caliper.runner.CaliperMain;
import com.google.caliper.runner.InvalidBenchmarkException;
import com.google.caliper.util.InvalidCommandException;
import groovyx.gpars.benchmark.caliper.akka.BenchmarkLatencyDynamicDispatchActorCaliper;
import groovyx.gpars.benchmark.caliper.akka.BenchmarkLatencyStaticDispatchActorCaliper;
import groovyx.gpars.benchmark.caliper.akka.BenchmarkThroughputComputationDynamicActorCaliper;
import groovyx.gpars.benchmark.caliper.akka.BenchmarkThroughputComputationStaticActorCaliper;
import groovyx.gpars.benchmark.caliper.akka.BenchmarkThroughputDynamicDispatchActorCaliper;
import groovyx.gpars.benchmark.caliper.akka.BenchmarkThroughputStaticDispatchActorCaliper;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;

import static com.google.common.collect.ObjectArrays.concat;

public final class BenchmarkRunner {

    public static void main(final String[] args) {
        final String[] latencyArg = {"-i", "latency"};
        final String[] throughputArg = {"-i", "throughput"};
        final PrintWriter writer = new PrintWriter(System.out);
        try {
            final Collection<Class> benchmarks = new ArrayList<Class>();

            benchmarks.add(BenchmarkLatencyDynamicDispatchActorCaliper.class);
            benchmarks.add(BenchmarkLatencyStaticDispatchActorCaliper.class);
            benchmarks.add(BenchmarkThroughputDynamicDispatchActorCaliper.class);
            benchmarks.add(BenchmarkThroughputStaticDispatchActorCaliper.class);
            benchmarks.add(BenchmarkThroughputComputationDynamicActorCaliper.class);
            benchmarks.add(BenchmarkThroughputComputationStaticActorCaliper.class);

            for (final Class benchmark : benchmarks) {
                try {
                    if (benchmark.getName().matches(".*Throughput.*")) {
                        CaliperMain.exitlessMain(concat(concat(throughputArg, args, String.class), benchmark.getName()), writer);
                    } else
                        CaliperMain.exitlessMain(concat(concat(latencyArg, args, String.class), benchmark.getName()), writer);
                } catch (InvalidCommandException e) {
                    e.display(writer);

                } catch (InvalidBenchmarkException e) {
                    e.display(writer);

                } catch (Throwable t) {
                    t.printStackTrace(writer);
                    writer.println();
                    writer.println("An unexpected exception has been thrown by the caliper runner.");
                    writer.println("Please see https://sites.google.com/site/caliperusers/issues");
                }

                writer.flush();
            }
        } finally {
            writer.close();
        }
    }
}
