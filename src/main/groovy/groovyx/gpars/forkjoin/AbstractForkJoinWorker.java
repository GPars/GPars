// GPars - Groovy Parallel Systems
//
// Copyright © 2008--2011  The original author or authors
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

package groovyx.gpars.forkjoin;

import jsr166y.RecursiveTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implements the ForkJoin worker contract.
 * Subclasses need to implement the compute() to perform the actual Fork/Join algorithm leveraging the options
 * provided by the AbstractForkJoinWorker class. The AbstractForJoinWorker class takes care of the child sub-processes.
 * <p/>
 * Author: Vaclav Pech
 * Date: Nov 1, 2009
 */
public abstract class AbstractForkJoinWorker<T> extends RecursiveTask<T> {

    /**
     * Stores the child workers
     */
    private List<AbstractForkJoinWorker<T>> children = null;

    protected AbstractForkJoinWorker() {
    }

    @Override
    protected final T compute() {
        try {
            return computeTask();
        } finally {
            children = null;
        }
    }

    protected abstract T computeTask();

    /**
     * Forks a child task. Makes sure it has a means to indicate back completion.
     * The worker is stored in the internal list of workers for evidence and easy result retrieval through getChildrenResults().
     *
     * @param child The child task
     */
    protected final void forkOffChild(final AbstractForkJoinWorker<T> child) {
        if (children == null) children = new ArrayList<AbstractForkJoinWorker<T>>();
        children.add(0, child);  //Forks and joins have to be processed in reverse order from one another
        child.fork();
    }

    /**
     * Waits for and returns the results of the child tasks.
     *
     * @return A list of results returned from the child tasks
     */
    public final List<T> getChildrenResults() {
        if (children == null) return Collections.emptyList();
        final List<T> results = new ArrayList<T>(children.size());
        for (final AbstractForkJoinWorker<T> worker : children) {
            results.add(worker.join());
        }
        return results;
    }
}
