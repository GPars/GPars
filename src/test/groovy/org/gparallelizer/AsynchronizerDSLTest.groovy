package org.gparallelizer

import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import static org.gparallelizer.Asynchronizer.*

/**
 * @author Vaclav Pech
 * Date: Nov 23, 2008
 */
public class AsynchronizerDSLTest extends GroovyTestCase {
    public void testDSLInitialization() {
        withAsynchronizer {
            assert ([2, 4, 6, 8, 10] == [1, 2, 3, 4, 5].collectAsync {it * 2})
            assert [1, 2, 3, 4, 5].allAsync {it > 0}
            assert [1, 2, 3, 4, 5].findAsync{Number number -> number > 2} in [3, 4, 5]
        }
        withAsynchronizer(5) {
            assert ([2, 4, 6, 8, 10] == [1, 2, 3, 4, 5].collectAsync {it * 2})
            assert [1, 2, 3, 4, 5].allAsync {it > 0}
            assert [1, 2, 3, 4, 5].findAsync{Number number -> number > 2} in [3, 4, 5]
        }

        def threadFactory={Runnable runnable ->
            Thread t = new Thread(runnable)
            t.daemon=false
            return t
        } as ThreadFactory
        
        withAsynchronizer(5, threadFactory) {
            assert ([2, 4, 6, 8, 10] == [1, 2, 3, 4, 5].collectAsync {it * 2})
            assert [1, 2, 3, 4, 5].allAsync {it > 0}
            assert [1, 2, 3, 4, 5].findAsync{Number number -> number > 2} in [3, 4, 5]
        }

        withExistingAsynchronizer(Executors.newFixedThreadPool(5)) {
            assert ([2, 4, 6, 8, 10] == [1, 2, 3, 4, 5].collectAsync {it * 2})
            assert [1, 2, 3, 4, 5].allAsync {it > 0}
            assert [1, 2, 3, 4, 5].findAsync{Number number -> number > 2} in [3, 4, 5]
        }
    }
}