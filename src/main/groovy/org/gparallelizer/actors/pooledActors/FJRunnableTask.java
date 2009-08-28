package org.gparallelizer.actors.pooledActors;

import jsr166y.forkjoin.AsyncAction;

/**
 * Adapts Runnables to the Fork Join framework.
 *
 * @author Vaclav Pech
 * Date: Jun 16, 2009
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
