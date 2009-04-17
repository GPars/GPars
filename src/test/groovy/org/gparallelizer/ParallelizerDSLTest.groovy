package org.gparallelizer

import java.lang.Thread.UncaughtExceptionHandler
import jsr166y.forkjoin.ForkJoinPool
import static org.gparallelizer.Parallelizer.*

/**
 * @author Vaclav Pech
 * Date: Nov 23, 2008
 */
public class ParallelizerDSLTest extends GroovyTestCase {
    public void testDSLInitialization() {
        withParallelizer {
            assert ([2, 4, 6, 8, 10] == [1, 2, 3, 4, 5].collectAsync {it * 2})
            assert [1, 2, 3, 4, 5].allAsync {it > 0}
            assert [1, 2, 3, 4, 5].findAsync{Number number -> number > 2} in [3, 4, 5]
        }
        withParallelizer(5) {
            assert ([2, 4, 6, 8, 10] == [1, 2, 3, 4, 5].collectAsync {it * 2})
            assert [1, 2, 3, 4, 5].allAsync {it > 0}
            assert [1, 2, 3, 4, 5].findAsync{Number number -> number > 2} in [3, 4, 5]
        }

        def handler = {Thread failedThread, Throwable throwable ->
            throwable.printStackTrace(System.err)
        } as UncaughtExceptionHandler

        withParallelizer(5, handler) {
            assert ([2, 4, 6, 8, 10] == [1, 2, 3, 4, 5].collectAsync {it * 2})
            assert [1, 2, 3, 4, 5].allAsync {it > 0}
            assert [1, 2, 3, 4, 5].findAsync{Number number -> number > 2} in [3, 4, 5]
        }

        withExistingParallelizer(new ForkJoinPool(5)) {
            assert ([2, 4, 6, 8, 10] == [1, 2, 3, 4, 5].collectAsync {it * 2})
            assert [1, 2, 3, 4, 5].allAsync {it > 0}
            assert [1, 2, 3, 4, 5].findAsync{Number number -> number > 2} in [3, 4, 5]
        }
    }
}