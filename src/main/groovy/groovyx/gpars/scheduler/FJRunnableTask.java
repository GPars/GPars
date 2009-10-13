//  GPars (formerly GParallelizer)
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License. 

package groovyx.gpars.scheduler;

import jsr166y.forkjoin.AsyncAction;

/**
 * Adapts Runnables to the Fork Join framework.
 *
 * @author Vaclav Pech
 *         Date: Jun 16, 2009
 */
final class FJRunnableTask extends AsyncAction {
    private final Runnable runnable;

    FJRunnableTask(final Runnable runnable) {
        this.runnable = runnable;
    }

    @Override protected void compute() {
        try {
            runnable.run();
            finish();
        } catch (Exception e) {
            finishExceptionally(e);
        }
    }
}
