//  GParallelizer
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

package org.gparallelizer

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit

/**
 * Enables a ExecutorService-based DSL on closures, objects and collections.
 * E.g.
 * Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
 *     Collection<Future> result = [1, 2, 3, 4, 5].collect({it * 10}.async())
 *     assertEquals(new HashSet([10, 20, 30, 40, 50]), new HashSet((Collection)result*.get()))
 * }
 *
 * Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
 *     def result = [1, 2, 3, 4, 5].findAsync{Number number -> number > 2}
 *     assert result in [3, 4, 5]
 * }
 *
 * @author Vaclav Pech
 * Date: Oct 23, 2008
 */
class Asynchronizer {

    private static ThreadLocal<ExecutorService> currentInvoker=new ThreadLocal<ExecutorService>()

    protected static ExecutorService retrieveCurrentPool() {
        currentInvoker.get()
    }

    private static createPool() {
        return createPool(Runtime.getRuntime().availableProcessors() + 1)
    }

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
            return thread
        } as ThreadFactory
    }

    /**
     * Creates a new instance of <i>ExecutorService</i>, binds it to the current thread, enables the ExecutorService DSL
     * and runs the supplied closure.
     * It is an identical alternative for withAsynchronizer() with a shorter name.
     * Within the supplied code block the <i>ExecutorService</i> is available as the only parameter, objects have been
     * enhanced with the <i>eachAsync()</i>, <i>collectAsync()</i> and other methods from the <i>AsyncInvokerUtil</i>
     * category class as well as closures can be turned into asynchronous ones by calling the <i>async()</i> method on them.
     * E.g. <i>closure,async</i> returns a new closure, which, when run will schedule the original closure
     * for processing in the pool.
     * Calling <i>images.eachAsync{processImage(it}}</i> will call the potentially long-lasting <i>processImage()</i>
     * operation on each image in the <i>images</i> collection in parallel.
     * <pre>
     * def result = new ConcurrentSkipListSet()
     * Asynchronizer.doAsync {ExecutorService service ->
     *     [1, 2, 3, 4, 5].eachAsync{Number number -> result.add(number * 10)}
     *     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     * }
     * </pre>
     * @param cl The block of code to invoke with the DSL enabled
     */
    public static doAsync(Closure cl) {
        return withAsynchronizer(cl)
    }

  /**
   * Creates a new instance of <i>ExecutorService</i>, binds it to the current thread, enables the ExecutorService DSL
   * and runs the supplied closure.
   * It is an identical alternative for withAsynchronizer() with a shorter name.
   * Within the supplied code block the <i>ExecutorService</i> is available as the only parameter, objects have been
   * enhanced with the <i>eachAsync()</i>, <i>collectAsync()</i> and other methods from the <i>AsyncInvokerUtil</i>
   * category class as well as closures can be turned into asynchronous ones by calling the <i>async()</i> method on them.
   * E.g. <i>closure,async</i> returns a new closure, which, when run will schedule the original closure
   * for processing in the pool.
   * Calling <i>images.eachAsync{processImage(it}}</i> will call the potentially long-lasting <i>processImage()</i>
   * operation on each image in the <i>images</i> collection in parallel.
   * <pre>
   * def result = new ConcurrentSkipListSet()
   * Asynchronizer.doAsync(5) {ExecutorService service ->
   *     [1, 2, 3, 4, 5].eachAsync{Number number -> result.add(number * 10)}
   *     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
   * }
   * </pre>
   * @param numberOfThreads Number of threads in the newly created thread pool
   * @param cl The block of code to invoke with the DSL enabled
   */
    public static doAsync(int numberOfThreads, Closure cl) {
        return withAsynchronizer(numberOfThreads, cl)
    }

    /**
     * Creates a new instance of <i>ExecutorService</i>, binds it to the current thread, enables the ExecutorService DSL
     * and runs the supplied closure.
     * It is an identical alternative for withAsynchronizer() with a shorter name.
     * Within the supplied code block the <i>ExecutorService</i> is available as the only parameter, objects have been
     * enhanced with the <i>eachAsync()</i>, <i>collectAsync()</i> and other methods from the <i>AsyncInvokerUtil</i>
     * category class as well as closures can be turned into asynchronous ones by calling the <i>async()</i> method on them.
     * E.g. <i>closure,async</i> returns a new closure, which, when run will schedule the original closure
     * for processing in the pool.
     * Calling <i>images.eachAsync{processImage(it}}</i> will call the potentially long-lasting <i>processImage()</i>
     * operation on each image in the <i>images</i> collection in parallel.
     * <pre>
     * def result = new ConcurrentSkipListSet()
     * Asynchronizer.doAsync(5) {ExecutorService service ->
     *     [1, 2, 3, 4, 5].eachAsync{Number number -> result.add(number * 10)}
     *     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     * }
     * </pre>
     * @param numberOfThreads Number of threads in the newly created thread pool
     * @param threadFactory Factory for threads in the pool
     * @param cl The block of code to invoke with the DSL enabled
     */
      public static doAsync(int numberOfThreads, ThreadFactory threadFactory, Closure cl) {
          return withAsynchronizer(numberOfThreads, threadFactory, cl)
      }

    /**
     * Creates a new instance of <i>ExecutorService</i>, binds it to the current thread, enables the ExecutorService DSL
     * and runs the supplied closure.
     * Within the supplied code block the <i>ExecutorService</i> is available as the only parameter, objects have been
     * enhanced with the <i>eachAsync()</i>, <i>collectAsync()</i> and other methods from the <i>AsyncInvokerUtil</i>
     * category class as well as closures can be turned into asynchronous ones by calling the <i>async()</i> method on them.
     * E.g. <i>closure,async</i> returns a new closure, which, when run will schedule the original closure
     * for processing in the pool.
     * Calling <i>images.eachAsync{processImage(it}}</i> will call the potentially long-lasting <i>processImage()</i>
     * operation on each image in the <i>images</i> collection in parallel.
     * <pre>
     * def result = new ConcurrentSkipListSet()
     * Asynchronizer.withAsynchronizer {ExecutorService service ->
     *     [1, 2, 3, 4, 5].eachAsync{Number number -> result.add(number * 10)}
     *     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     * }
     * </pre>
     * @param cl The block of code to invoke with the DSL enabled
     */
    public static withAsynchronizer(Closure cl) {
        return withAsynchronizer(3, cl)
    }

  /**
   * Creates a new instance of <i>ExecutorService</i>, binds it to the current thread, enables the ExecutorService DSL
   * and runs the supplied closure.
   * Within the supplied code block the <i>ExecutorService</i> is available as the only parameter, objects have been
   * enhanced with the <i>eachAsync()</i>, <i>collectAsync()</i> and other methods from the <i>AsyncInvokerUtil</i>
   * category class as well as closures can be turned into asynchronous ones by calling the <i>async()</i> method on them.
   * E.g. <i>closure,async</i> returns a new closure, which, when run will schedule the original closure
   * for processing in the pool.
   * Calling <i>images.eachAsync{processImage(it}}</i> will call the potentially long-lasting <i>processImage()</i>
   * operation on each image in the <i>images</i> collection in parallel.
   * <pre>
   * def result = new ConcurrentSkipListSet()
   * Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
   *     [1, 2, 3, 4, 5].eachAsync{Number number -> result.add(number * 10)}
   *     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
   * }
   * </pre>
   * @param numberOfThreads Number of threads in the newly created thread pool
   * @param cl The block of code to invoke with the DSL enabled
   */
    public static withAsynchronizer(int numberOfThreads, Closure cl) {
        return withAsynchronizer(numberOfThreads, createDefaultThreadFactory(), cl)
    }

  /**
   * Creates a new instance of <i>ExecutorService</i>, binds it to the current thread, enables the ExecutorService DSL
   * and runs the supplied closure.
   * Within the supplied code block the <i>ExecutorService</i> is available as the only parameter, objects have been
   * enhanced with the <i>eachAsync()</i>, <i>collectAsync()</i> and other methods from the <i>AsyncInvokerUtil</i>
   * category class as well as closures can be turned into asynchronous ones by calling the <i>async()</i> method on them.
   * E.g. <i>closure,async</i> returns a new closure, which, when run will schedule the original closure
   * for processing in the pool.
   * Calling <i>images.eachAsync{processImage(it}}</i> will call the potentially long-lasting <i>processImage()</i>
   * operation on each image in the <i>images</i> collection in parallel.
   * <pre>
   * def result = new ConcurrentSkipListSet()
   * Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
   *     [1, 2, 3, 4, 5].eachAsync{Number number -> result.add(number * 10)}
   *     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
   * }
   * </pre>
   * @param numberOfThreads Number of threads in the newly created thread pool
   * @param threadFactory Factory for threads in the pool
   * @param cl The block of code to invoke with the DSL enabled
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
   * enhanced with the <i>eachAsync()</i>, <i>collectAsync()</i> and other methods from the <i>AsyncInvokerUtil</i>
   * category class as well as closures can be turned into asynchronous ones by calling the <i>async()</i> method on them.
   * E.g. <i>closure,async</i> returns a new closure, which, when run will schedule the original closure
   * for processing in the pool.
   * Calling <i>images.eachAsync{processImage(it}}</i> will call the potentially long-lasting <i>processImage()</i>
   * operation on each image in the <i>images</i> collection in parallel.
   * <pre>
   * def result = new ConcurrentSkipListSet()
   * Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
   *     [1, 2, 3, 4, 5].eachAsync{Number number -> result.add(number * 10)}
   *     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
   * }
   * </pre>
   * @param pool The <i>ExecutorService</i> to use, the service will not be shutdown after this method returns
   */
    public static withExistingAsynchronizer(ExecutorService pool, Closure cl) {
        currentInvoker.set(pool)
        def result=null
       try {
            use(AsyncInvokerUtil) {
                result = cl(pool)
            }
        } finally {
            currentInvoker.remove()
        }
        return result
    }
}
