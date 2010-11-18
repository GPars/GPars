// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
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

package org.codehaus.gpars.javademo.benchmark;

import groovyx.gpars.MessagingRunnable;
import groovyx.gpars.agent.Agent;
import groovyx.gpars.dataflow.DataFlow;
import groovyx.gpars.dataflow.DataFlowVariable;

public class QBenchmarkGParsDataFlowAndAgent {
    //  A singleton constant object used for all the synchronization.  As pointed out by Roger Orr (private
    //  communication, 2010-04-19) there is a subtle bug in using the sum variable (if it were object) to synchronize
    //  actions on itself since the variable refers to a different object after the update operation compared
    //  to the one it refers to when the object lock is claimed.
    private static Agent<Accumulator> sum;

    private static void execute(final int numberOfTasks) throws InterruptedException {
        final long n = 1000000000l;
        final double delta = 1.0 / n;
        final long startTimeNanos = System.nanoTime();
        final long sliceSize = n / numberOfTasks;
        final DataFlowVariable<?>[] tasks = new DataFlowVariable[numberOfTasks];
        sum = new Agent<Accumulator>(new Accumulator());
        for (int i = 0; i < numberOfTasks; ++i) {
            final int id = i;
            tasks[id] = DataFlow.task(new Runnable() {
                public void run() {
                    final long start = 1 + id * sliceSize;
                    final long end = (id + 1) * sliceSize;
                    double localSum = 0.0;
                    for (long i = start; i <= end; ++i) {
                        final double x = (i - 0.5d) * delta;
                        localSum += 1.0 / (1.0 + x * x);
                    }
                    final double currentSum = localSum;
                    sum.send(new MessagingRunnable<Accumulator>() {
                        @Override
                        protected void doRun(final Accumulator t) {
                            t.add(currentSum);
                        }
                    });
                }
            }
            );
        }
        for (final DataFlowVariable<?> t : tasks) {
            try {
                t.join();
            }
            catch (final InterruptedException ie) {
                throw new RuntimeException("Got an InterruptedException joining a thread.", ie);
            }
        }
        final double pi = 4.0 * sum.getVal().getSum() * delta;
        final double elapseTime = (System.nanoTime() - startTimeNanos) / 1e9;
        System.out.println("==== Groovy/Java GPars Dataflow/Agent pi = " + pi);
        System.out.println("==== Groovy/Java GPars Dataflow/Agent iteration count = " + n);
        System.out.println("==== Groovy/Java GPars Dataflow/Agent elapse = " + elapseTime);
        System.out.println("==== Groovy/Java GPars Dataflow/Agent processor count = " + Runtime.getRuntime().availableProcessors());
        System.out.println("==== Groovy/Java GPars Dataflow/Agent thread count = " + numberOfTasks);
    }

    static class Accumulator {
        private double sum = 0.0;

        void add(final double value) {
            sum += value;
        }

        public double getSum() {
            return sum;
        }
    }

    public static void main
            (
                    final String[] args) throws InterruptedException {
        QBenchmarkGParsDataFlowAndAgent.execute(1);
        QBenchmarkGParsDataFlowAndAgent.execute(1);
        QBenchmarkGParsDataFlowAndAgent.execute(1);
        System.out.println();
        QBenchmarkGParsDataFlowAndAgent.execute(2);
        System.out.println();
        QBenchmarkGParsDataFlowAndAgent.execute(8);
        System.out.println();
        QBenchmarkGParsDataFlowAndAgent.execute(32);
    }
}
