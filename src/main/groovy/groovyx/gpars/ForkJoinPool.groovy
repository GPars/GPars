// GPars (formerly GParallelizer)
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

package groovyx.gpars

import groovyx.gpars.util.PoolUtils
import java.lang.Thread.UncaughtExceptionHandler
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import jsr166y.forkjoin.RecursiveTask

/**
 * Enables a ParallelArray-based (from JSR-166y) DSL on collections. In general cases the Parallel Array implementation
 * shows to be much faster (10 - 20 times) compared to the executor service implementation in ThreadPool.
 * E.g.
 <pre>
 ForkJoinPool.withPool(5) {final AtomicInteger result = new AtomicInteger(0)
 [1, 2, 3, 4, 5].eachParallel {result.addAndGet(it)}assertEquals 15, result}ForkJoinPool.withPool(5) {final List result = [1, 2, 3, 4, 5].collectParallel {it * 2}assert ([2, 4, 6, 8, 10].equals(result))}ForkJoinPool.withPool(5) {assert [1, 2, 3, 4, 5].everyParallel {it > 0}assert ![1, 2, 3, 4, 5].everyParallel {it > 1}}</pre>
 * @author Vaclav Pech
 * Date: Oct 23, 2008
 */
public class ForkJoinPool {

    /**
     * Maps threads to their appropriate thread pools
     */
    private static final ThreadLocalPools currentPoolStack = new ThreadLocalPools()

    /**
     * Caches the default pool size.
     */
    private static final int defaultPoolSize = PoolUtils.retrieveDefaultPoolSize()

    /**
     * Retrieves the pool assigned to the current thread.
     */
    protected static retrieveCurrentPool() {
        currentPoolStack.current
    }

    /**
     * Creates a new pool with the default size()
     */
    private static createPool() {
        return createPool(PoolUtils.retrieveDefaultPoolSize())
    }

    /**
     * Creates a new pool with the given size()
     */
    private static createPool(int poolSize) {
        return createPool(poolSize, createDefaultUncaughtExceptionHandler())
    }

    private static createPool(int poolSize, UncaughtExceptionHandler handler) {
        if (!(poolSize in 1..Integer.MAX_VALUE)) throw new IllegalArgumentException("Invalid value $poolSize for the pool size has been specified. Please supply a positive int number.")
        final jsr166y.forkjoin.ForkJoinPool pool = new jsr166y.forkjoin.ForkJoinPool(poolSize)
        pool.uncaughtExceptionHandler = handler
        return pool
    }

    /**
     * Creates a new instance of <i>ForkJoinPool</i>, binds it to the current thread, enables the ParallelArray DSL
     * and runs the supplied closure.
     * Within the supplied code block the <i>ForkJoinPool</i> is available as the only parameter, collections have been
     * enhanced with the <i>eachParallel()</i>, <i>collectParallel()</i> and other methods from the <i>ParallelArrayUtil</i>
     * category class.
     * E.g. calling <i>images.eachParallel{processImage(it}}</i> will call the potentially long-lasting <i>processImage()</i>
     * operation on each image in the <i>images</i> collection in parallel.
     * Be sure to synchronize all modifiable state shared by the asynchronously running closures.
     * <pre>
     * ForkJoinPool.withPool {ForkJoinPool pool ->
     *     def result = Collections.synchronizedSet(new HashSet())
     *     [1, 2, 3, 4, 5].eachParallel {Number number -> result.add(number * 10)}*     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     *}* </pre>
     * @param cl The block of code to invoke with the DSL enabled
     */
    public static withPool(Closure cl) {
        return withPool(defaultPoolSize, cl)
    }

    /**
     * Creates a new instance of <i>ForkJoinPool</i>, binds it to the current thread, enables the ParallelArray DSL
     * and runs the supplied closure.
     * Within the supplied code block the <i>ForkJoinPool</i> is available as the only parameter, collections have been
     * enhanced with the <i>eachParallel()</i>, <i>collectParallel()</i> and other methods from the <i>ParallelArrayUtil</i>
     * category class.
     * E.g. calling <i>images.eachParallel{processImage(it}}</i> will call the potentially long-lasting <i>processImage()</i>
     * operation on each image in the <i>images</i> collection in parallel.
     * Be sure to synchronize all modifiable state shared by the asynchronously running closures.
     * <pre>
     * ForkJoinPool.withPool(5) {ForkJoinPool pool ->
     *     def result = Collections.synchronizedSet(new HashSet())
     *     [1, 2, 3, 4, 5].eachParallel {Number number -> result.add(number * 10)}*     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     *}* </pre>
     * @param numberOfThreads Number of threads in the newly created thread pool
     * @param cl The block of code to invoke with the DSL enabled
     */
    public static withPool(int numberOfThreads, Closure cl) {
        return withPool(numberOfThreads, createDefaultUncaughtExceptionHandler(), cl)
    }

    /**
     * Creates a new instance of <i>ForkJoinPool</i>, binds it to the current thread, enables the ParallelArray DSL
     * and runs the supplied closure.
     * Within the supplied code block the <i>ForkJoinPool</i> is available as the only parameter, collections have been
     * enhanced with the <i>eachParallel()</i>, <i>collectParallel()</i> and other methods from the <i>ParallelArrayUtil</i>
     * category class.
     * E.g. calling <i>images.eachParallel{processImage(it}}</i> will call the potentially long-lasting <i>processImage()</i>
     * operation on each image in the <i>images</i> collection in parallel.
     * Be sure to synchronize all modifiable state shared by the asynchronously running closures.
     * <pre>
     * ForkJoinPool.withPool(5, handler) {ForkJoinPool pool ->
     *     def result = Collections.synchronizedSet(new HashSet())
     *     [1, 2, 3, 4, 5].eachParallel {Number number -> result.add(number * 10)}*     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     *}* </pre>
     * @param numberOfThreads Number of threads in the newly created thread pool
     * @param handler Handler for uncaught exceptions raised in code performed by the pooled threads
     * @param cl The block of code to invoke with the DSL enabled
     */
    public static withPool(int numberOfThreads, UncaughtExceptionHandler handler, Closure cl) {
        final jsr166y.forkjoin.ForkJoinPool pool = createPool(numberOfThreads, handler)
        try {
            return withExistingPool(pool, cl)
        } finally {
            pool.shutdown()
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)
        }
    }

    /**
     * @deprecated
     */
    public static doParallel(Closure cl) {
        return withPool(defaultPoolSize, cl)
    }

    /**
     * @deprecated
     */
    public static doParallel(int numberOfThreads, Closure cl) {
        return withPool(numberOfThreads, createDefaultUncaughtExceptionHandler(), cl)
    }

    /**
     * @deprecated
     */
    public static doParallel(int numberOfThreads, UncaughtExceptionHandler handler, Closure cl) {
        final ForkJoinPool pool = createPool(numberOfThreads, handler)
        try {
            return withExistingPool(pool, cl)
        } finally {
            pool.shutdown()
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)
        }
    }

    /**
     * Reuses an instance of <i>ForkJoinPool</i>, binds it to the current thread, enables the ParallelArray DSL
     * and runs the supplied closure.
     * Within the supplied code block the <i>ForkJoinPool</i> is available as the only parameter, collections have been
     * enhanced with the <i>eachParallel()</i>, <i>collectParallel()</i> and other methods from the <i>ParallelArrayUtil</i>
     * category class.
     * E.g. calling <i>images.eachParallel{processImage(it}}</i> will call the potentially long-lasting <i>processImage()</i>
     * operation on each image in the <i>images</i> collection in parallel.
     * Be sure to synchronize all modifiable state shared by the asynchronously running closures.
     * <pre>
     * ForkJoinPool.withExistingPool(anotherPool) {ForkJoinPool pool ->
     *     def result = Collections.synchronizedSet(new HashSet())
     *     [1, 2, 3, 4, 5].eachParallel {Number number -> result.add(number * 10)}*     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     *}*  </pre>
     * @param pool The thread pool to use, the pool will not be shutdown after this method returns
     */
    public static withExistingPool(jsr166y.forkjoin.ForkJoinPool pool, Closure cl) {

        currentPoolStack << pool
        def result = null
        try {
            use(ParallelArrayUtil) {
                result = cl(pool)
            }
        } finally {
            currentPoolStack.pop()
        }
        return result
    }

    /**
     * Just like withExistingPool() registers a thread pool, but doesn't install the ParallelArrayUtil category.
     * Used by ParallelEnhancer's Parallel mixins. 
     */
    static ensurePool(final jsr166y.forkjoin.ForkJoinPool pool, final Closure cl) {
        currentPoolStack << pool
        try {
            return cl(pool)
        } finally {
            currentPoolStack.pop()
        }
    }

    /**
     * Starts a ForkJoin calculation with the supplied root worker and waits for the result.
     * @param rootWorker The worker that calculates the root of the Fork/Join problem
     * @return The result of the whole calculation
     */
    public static <T> T orchestrate(final AbstractForkJoinWorker<T> rootWorker) {
        def pool = ForkJoinPool.retrieveCurrentPool()
        if (pool == null) throw new IllegalStateException("Cannot initialize ForkJoin. The pool has not been set. Perhaps, we're not inside a ForkJoinPool.withPool() block.")
        return pool.submit(rootWorker).get()
    }

    /**
     * Starts multiple closures in separate threads, collecting their return values
     * If an exception is thrown from the closure when called on any of the collection's elements,
     * it will be re-thrown in the calling thread when it calls the Future.get() method.
     * @return The result values of all closures
     * @throws AsyncException If any of the collection's elements causes the closure to throw an exception. The original exceptions will be stored in the AsyncException's concurrentExceptions field.
     */
    public static List<Object> doInParallel(Closure... closures) {
        return AsyncInvokerUtil.processResult(executeAsync(closures))
    }

    /**
     * Starts multiple closures in separate threads, collecting their return values
     * If an exception is thrown from the closure when called on any of the collection's elements,
     * it will be re-thrown in the calling thread when it calls the Future.get() method.
     * @return The result values of all closures
     * @throws AsyncException If any of the collection's elements causes the closure to throw an exception. The original exceptions will be stored in the AsyncException's concurrentExceptions field.
     */
    public static List<Object> doInParallel(List<Closure> closures) {
        return doInParallel(* closures)
    }

    /**
     * Starts multiple closures in separate threads, collecting Futures for their return values
     * If an exception is thrown from the closure when called on any of the collection's elements,
     * it will be re-thrown in the calling thread when it calls the Future.get() method.
     * @return Futures for the result values or exceptions of all closures
     */
    public static List<Future<Object>> executeAsync(Closure... closures) {
        jsr166y.forkjoin.ForkJoinPool pool = retrieveCurrentPool()
        if (pool == null) throw new IllegalStateException("No active Fork/Join thread pool available to execute closures asynchronously.")
        List<Future<Object>> result = closures.collect {cl ->
            pool.submit([compute: { cl.call() }] as RecursiveTask)
        }
        result
    }

    /**
     * Starts multiple closures in separate threads, collecting Futures for their return values
     * If an exception is thrown from the closure when called on any of the collection's elements,
     * it will be re-thrown in the calling thread when it calls the Future.get() method.
     * @return Futures for the result values or exceptions of all closures
     */
    public static List<Future<Object>> executeAsync(List<Closure> closures) {
        return executeAsync(* closures)
    }

    private static UncaughtExceptionHandler createDefaultUncaughtExceptionHandler() {
        return {Thread failedThread, Throwable throwable ->
            System.err.println "Error processing background thread ${failedThread.name}: ${throwable.message}"
            throwable.printStackTrace(System.err)
        } as UncaughtExceptionHandler
    }
}
