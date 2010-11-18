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

public class ProcessSlice {
    private final int taskId;
    private final long sliceSize;
    private final double delta;

    public ProcessSlice(final int taskId, final long sliceSize, final double delta) {
        this.taskId = taskId;
        this.sliceSize = sliceSize;
        this.delta = delta;
    }

    public double compute() {
        final long start = 1 + taskId * sliceSize;
        final long end = (taskId + 1) * sliceSize;
        double sum = 0.0;
        for (long j = start; j <= end; ++j) {
            final double x = (j - 0.5d) * delta;
            sum += 1.0d / (1.0d + x * x);
        }
        return sum;
    }
}
