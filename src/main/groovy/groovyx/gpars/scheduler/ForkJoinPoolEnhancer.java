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

package groovyx.gpars.scheduler;

import jsr166y.ForkJoinPool;

/**
 * Enhanced the ForkJoinPool class with the resizing capability
 */
public class ForkJoinPoolEnhancer extends ForkJoinPool {
    public ForkJoinPoolEnhancer() {
    }

    public ForkJoinPoolEnhancer(final int parallelism) {
        super(parallelism);
    }

    public ForkJoinPoolEnhancer(final int parallelism, final ForkJoinWorkerThreadFactory factory, final Thread.UncaughtExceptionHandler handler, final boolean asyncMode) {
        super(parallelism, factory, handler, asyncMode);
    }

    void addPoolActiveCount(final int delta) {
//        addActiveCount(delta);
    }
}
