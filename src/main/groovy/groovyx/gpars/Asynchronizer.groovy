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

package groovyx.gpars

import groovyx.gpars.util.PoolUtils
import java.lang.Thread.UncaughtExceptionHandler
import org.codehaus.groovy.runtime.InvokerInvocationException
import java.util.concurrent.*

/**
 * Enables a ExecutorService-based DSL on closures, objects and collections.
 * E.g.
 * Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
 *     Collection<Future> result = [1, 2, 3, 4, 5].collectParallel({it * 10}.async())
 *     assertEquals(new HashSet([10, 20, 30, 40, 50]), new HashSet((Collection)result*.get()))
 *}*
 * Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
 *     def result = [1, 2, 3, 4, 5].findParallel{Number number -> number > 2}*     assert result in [3, 4, 5]
 *}*
 * @author Vaclav Pech
 * Date: Oct 23, 2008
 */
class Asynchronizer {

    /**
     * Maps threads to their appropriate thread pools
     */
    private static final ThreadLocal<ExecutorService> currentInvoker = new ThreadLocal<ExecutorService>()

    /**
     * Caches the default pool size.
     */
    private static final int defaultPoolSize = PoolUtils.retrieveDefaultPoolSize()

    /**
     * Retrieves the pool assigned to the current thread.
     */
    protected static ExecutorService retrieveCurrentPool() {
        currentInvoker.get()
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
        return createPool(poolSize, createDefaultThreadFactory())
    }

    private static createPool(int poolSize, ThreadFactory threadFactory) {
        if (!(poolSize in 1..Integer.MAX_VALUE)) throw new IllegalArgumentException("Invalid value $poolSize for the pool size has been specified. Please supply a positive int number.")
        if (!threadFactory) throw new IllegalArgumentException("No value specified for threadFactory.")
        return Executors.newFixedThreadPool(poolSize, threadFactory)
    }

    private static ThreadFactory createDefaultThreadFactory() {
        return {Runnable runnable ->
            final Thread thread = new Thread(runnable)
            thread.daemon = false
            thread
        } as ThreadFactory
    }

    /**
     * Creates a new instance of <i>ExecutorService</i>, binds it to the current thread, enables the ExecutorService DSL
     * and runs the supplied closure.
     * It is an identical alternative for withAsynchronizer() with a shorter name.
     * Within the supplied code block the <i>ExecutorService</i> is available as the only parameter, objects have been
     * enhanced with the <i>eachParallel()</i>, <i>collectParallel()</i> and other methods from the <i>AsyncInvokerUtil</i>
     * category class as well as closures can be turned into asynchronous ones by calling the <i>async()</i> method on them.
     * E.g. <i>closure,async</i> returns a new closure, which, when run will schedule the original closure
     * for processing in the pool.
     * Calling <i>images.eachParallel{processImage(it}}</i> will call the potentially long-lasting <i>processImage()</i>
     * operation on each image in the <i>images</i> collection in parallel.
     * <pre>
     * def result = new ConcurrentSkipListSet()
     * Asynchronizer.doParallel {ExecutorService service ->
     *     [1, 2, 3, 4, 5].eachParallel{Number number -> result.add(number * 10)}*     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     *}* </pre>
     * @param cl The block of code to invoke with the DSL enabled
     */
    public static doParallel(Closure cl) {
        return doParallel(defaultPoolSize, cl)
    }

    /**
     * Creates a new instance of <i>ExecutorService</i>, binds it to the current thread, enables the ExecutorService DSL
     * and runs the supplied closure.
     * It is an identical alternative for withAsynchronizer() with a shorter name.
     * Within the supplied code block the <i>ExecutorService</i> is available as the only parameter, objects have been
     * enhanced with the <i>eachParallel()</i>, <i>collectParallel()</i> and other methods from the <i>AsyncInvokerUtil</i>
     * category class as well as closures can be turned into asynchronous ones by calling the <i>async()</i> method on them.
     * E.g. <i>closure,async</i> returns a new closure, which, when run will schedule the original closure
     * for processing in the pool.
     * Calling <i>images.eachParallel{processImage(it}}</i> will call the potentially long-lasting <i>processImage()</i>
     * operation on each image in the <i>images</i> collection in parallel.
     * <pre>
     * def result = new ConcurrentSkipListSet()
     * Asynchronizer.doParallel(5) {ExecutorService service ->
     *     [1, 2, 3, 4, 5].eachParallel{Number number -> result.add(number * 10)}*     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     *}* </pre>
     * @param numberOfThreads Number of threads in the newly created thread pool
     * @param cl The block of code to invoke with the DSL enabled
     */
    public static doParallel(int numberOfThreads, Closure cl) {
        return doParallel(numberOfThreads, createDefaultThreadFactory(), cl)
    }

    /**
     * Creates a new instance of <i>ExecutorService</i>, binds it to the current thread, enables the ExecutorService DSL
     * and runs the supplied closure.
     * It is an identical alternative for withAsynchronizer() with a shorter name.
     * Within the supplied code block the <i>ExecutorService</i> is available as the only parameter, objects have been
     * enhanced with the <i>eachParallel()</i>, <i>collectParallel()</i> and other methods from the <i>AsyncInvokerUtil</i>
     * category class as well as closures can be turned into asynchronous ones by calling the <i>async()</i> method on them.
     * E.g. <i>closure,async</i> returns a new closure, which, when run will schedule the original closure
     * for processing in the pool.
     * Calling <i>images.eachParallel{processImage(it}}</i> will call the potentially long-lasting <i>processImage()</i>
     * operation on each image in the <i>images</i> collection in parallel.
     * <pre>
     * def result = new ConcurrentSkipListSet()
     * Asynchronizer.doParallel(5) {ExecutorService service ->
     *     [1, 2, 3, 4, 5].eachParallel{Number number -> result.add(number * 10)}*     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     *}* </pre>
     * @param numberOfThreads Number of threads in the newly created thread pool
     * @param threadFactory Factory for threads in the pool
     * @param cl The block of code to invoke with the DSL enabled
     */
    public static doParallel(int numberOfThreads, ThreadFactory threadFactory, Closure cl) {
        final ExecutorService pool = createPool(numberOfThreads, threadFactory)
        try {
            return withExistingAsynchronizer(pool, cl)
        } finally {
            pool.shutdown()
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)
        }

    }

    /**
     * Creates a new instance of <i>ExecutorService</i>, binds it to the current thread, enables the ExecutorService DSL
     * and runs the supplied closure.
     * Within the supplied code block the <i>ExecutorService</i> is available as the only parameter, objects have been
     * enhanced with the <i>eachParallel()</i>, <i>collectParallel()</i> and other methods from the <i>AsyncInvokerUtil</i>
     * category class as well as closures can be turned into asynchronous ones by calling the <i>async()</i> method on them.
     * E.g. <i>closure,async</i> returns a new closure, which, when run will schedule the original closure
     * for processing in the pool.
     * Calling <i>images.eachParallel{processImage(it}}</i> will call the potentially long-lasting <i>processImage()</i>
     * operation on each image in the <i>images</i> collection in parallel.
     * <pre>
     * def result = new ConcurrentSkipListSet()
     * Asynchronizer.withAsynchronizer {ExecutorService service ->
     *     [1, 2, 3, 4, 5].eachParallel{Number number -> result.add(number * 10)}*     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     *}* </pre>
     * @param cl The block of code to invoke with the DSL enabled
     * @deprecated Use doParallel() instead
     */
    public static withAsynchronizer(Closure cl) {
        return withAsynchronizer(3, cl)
    }

    /**
     * Creates a new instance of <i>ExecutorService</i>, binds it to the current thread, enables the ExecutorService DSL
     * and runs the supplied closure.
     * Within the supplied code block the <i>ExecutorService</i> is available as the only parameter, objects have been
     * enhanced with the <i>eachParallel()</i>, <i>collectParallel()</i> and other methods from the <i>AsyncInvokerUtil</i>
     * category class as well as closures can be turned into asynchronous ones by calling the <i>async()</i> method on them.
     * E.g. <i>closure,async</i> returns a new closure, which, when run will schedule the original closure
     * for processing in the pool.
     * Calling <i>images.eachParallel{processImage(it}}</i> will call the potentially long-lasting <i>processImage()</i>
     * operation on each image in the <i>images</i> collection in parallel.
     * <pre>
     * def result = new ConcurrentSkipListSet()
     * Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     *     [1, 2, 3, 4, 5].eachParallel{Number number -> result.add(number * 10)}*     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     *}* </pre>
     * @param numberOfThreads Number of threads in the newly created thread pool
     * @param cl The block of code to invoke with the DSL enabled
     * @deprecated Use doParallel() instead
     */
    public static withAsynchronizer(int numberOfThreads, Closure cl) {
        return withAsynchronizer(numberOfThreads, createDefaultThreadFactory(), cl)
    }

    /**
     * Creates a new instance of <i>ExecutorService</i>, binds it to the current thread, enables the ExecutorService DSL
     * and runs the supplied closure.
     * Within the supplied code block the <i>ExecutorService</i> is available as the only parameter, objects have been
     * enhanced with the <i>eachParallel()</i>, <i>collectParallel()</i> and other methods from the <i>AsyncInvokerUtil</i>
     * category class as well as closures can be turned into asynchronous ones by calling the <i>async()</i> method on them.
     * E.g. <i>closure,async</i> returns a new closure, which, when run will schedule the original closure
     * for processing in the pool.
     * Calling <i>images.eachParallel{processImage(it}}</i> will call the potentially long-lasting <i>processImage()</i>
     * operation on each image in the <i>images</i> collection in parallel.
     * <pre>
     * def result = new ConcurrentSkipListSet()
     * Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     *     [1, 2, 3, 4, 5].eachParallel{Number number -> result.add(number * 10)}*     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     *}* </pre>
     * @param numberOfThreads Number of threads in the newly created thread pool
     * @param threadFactory Factory for threads in the pool
     * @param cl The block of code to invoke with the DSL enabled
     * @deprecated Use doParallel() instead
     */
    public static withAsynchronizer(int numberOfThreads, ThreadFactory threadFactory, Closure cl) {
        final ExecutorService pool = createPool(numberOfThreads, threadFactory)
        try {
            return withExistingAsynchronizer(pool, cl)
        } finally {
            pool.shutdown()
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)
        }
    }

    /**
     * Creates a new instance of <i>ExecutorService</i>, binds it to the current thread, enables the ExecutorService DSL
     * and runs the supplied closure.
     * Within the supplied code block the <i>ExecutorService</i> is available as the only parameter, objects have been
     * enhanced with the <i>eachParallel()</i>, <i>collectParallel()</i> and other methods from the <i>AsyncInvokerUtil</i>
     * category class as well as closures can be turned into asynchronous ones by calling the <i>async()</i> method on them.
     * E.g. <i>closure,async</i> returns a new closure, which, when run will schedule the original closure
     * for processing in the pool.
     * Calling <i>images.eachParallel{processImage(it}}</i> will call the potentially long-lasting <i>processImage()</i>
     * operation on each image in the <i>images</i> collection in parallel.
     * <pre>
     * def result = new ConcurrentSkipListSet()
     * Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     *     [1, 2, 3, 4, 5].eachParallel{Number number -> result.add(number * 10)}*     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     *}* </pre>
     * @param pool The <i>ExecutorService</i> to use, the service will not be shutdown after this method returns
     */
    public static withExistingAsynchronizer(ExecutorService pool, Closure cl) {
        currentInvoker.set(pool)
        def result = null
        try {
            use(AsyncInvokerUtil) {
                result = cl(pool)
            }
        } finally {
            currentInvoker.remove()
        }
        return result
    }

    /**
     * Starts multiple closures in separate threads, collecting their return values
     * If an exception is thrown from the closure when called on any of the collection's elements,
     * it will be rethrown in the calling thread when it calls the Future.get() method.
     * @return The result values of all closures
     * @throws AsyncException If any of the collection's elements causes the closure to throw an exception. The original exceptions will be stored in the AsyncException's concurrentExceptions field.
     */
    public static List<Object> doInParallel(Closure ... closures) {
        return AsyncInvokerUtil.processResult(executeAsync(closures))
    }

    /**
     * Starts multiple closures in separate threads, collecting Futures for their return values
     * If an exception is thrown from the closure when called on any of the collection's elements,
     * it will be rethrown in the calling thread when it calls the Future.get() method.
     * @return Futures for the result values or exceptions of all closures
    */
    public static List<Future<Object>> executeAsync(Closure ... closures) {
        Asynchronizer.withAsynchronizer(closures.size()) {ExecutorService executorService ->
            List<Future<Object>> result = closures.collect {cl ->
                executorService.submit({
                    cl.call()
                } as Callable<Object>)
            }
            result
        }
    }

    /**
     * Starts multiple closures in separate threads, using a new thread for the startup.
     * If any of the collection's elements causes the closure to throw an exception, an AsyncException is reported using System.err.
     * The original exceptions will be stored in the AsyncException's concurrentExceptions field.
     */
    public static void startInParallel(Closure ... closures) {
        startInParallel(createDefaultUncaughtExceptionHandler(), closures)
    }

    /**
     * Starts multiple closures in separate threads, using a new thread for the startup.
     * If any of the collection's elements causes the closure to throw an exception, an AsyncException is reported to the supplied instance of UncaughtExceptionHandler.
     * The original exceptions will be stored in the AsyncException's concurrentExceptions field.
     * Unwraps potential InvokerInvocationException before control is passed to the UncaughtExceptionHandler instance.
     * @return The thread that submits the closures to the thread executor service so that the caller can take ownership of it and e.g. call <i>join()</i> on it to wait for all the closures to finish processing.
     */
    public static Thread startInParallel(java.lang.Thread.UncaughtExceptionHandler uncaughtExceptionHandler, Closure ... closures) {
        final Thread thread = new Thread({
            doInParallel(closures)
        } as Runnable)
        thread.daemon = false
        thread.uncaughtExceptionHandler = {Thread t, Throwable throwable ->
            if (throwable instanceof InvokerInvocationException)
                uncaughtExceptionHandler.uncaughtException(t, throwable.cause)
            else
                uncaughtExceptionHandler.uncaughtException(t, throwable)
        } as UncaughtExceptionHandler
        thread.start()
        return thread
    }

    private static UncaughtExceptionHandler createDefaultUncaughtExceptionHandler() {
        return {Thread failedThread, Throwable throwable ->
            System.err.println "Error processing background thread ${failedThread.name}: ${throwable.message}"
            throwable.printStackTrace(System.err)
        } as UncaughtExceptionHandler
    }
}
