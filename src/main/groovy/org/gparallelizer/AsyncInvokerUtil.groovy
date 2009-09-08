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

import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.lang.Thread.UncaughtExceptionHandler
import java.util.concurrent.Semaphore

/**
 * This class forms the core of the DSL initialized by <i>Asynchronizer</i>. The static methods of <i>AsyncInvokerUtil</i>
 * get attached to their first arguments (the Groovy Category mechanism) and can be then invoked as if they were part of
 * the argument classes.
 * @see org.gparallelizer.Asynchronizer
 *
 * @author Vaclav Pech
 * Date: Oct 23, 2008
 */
public class AsyncInvokerUtil {
    /**
     * schedules the supplied closure for processing in the underlying thread pool.
     */
    private static Future callAsync(Closure task) {
        final ExecutorService pool = Asynchronizer.retrieveCurrentPool()
        if (!pool) throw new IllegalStateException("No ExecutorService available for the current thread.")
        return pool.submit(task as Callable)
    }

    /**
     * Calls a closure in a separate thread supplying the given arguments, returning a future for the potential return value,
     */
    public static Future callAsync(final Closure cl, final Object ... args) {
        callAsync {->cl(*args)}
    }

    /**
     * Submits the task for asynchronous processing returning the Future received from the executor service.
     * Allows for the followitn syntax:
     * <pre>
     * executorService << {println 'Inside parallel task'}* </pre>
     */
    public static Future leftShift(ExecutorService executorService, Closure task) {
        return executorService.submit(task as Callable)
    }

    /**
     * Creates an asynchronous variant of the supplied closure, which, when invoked returns a future for the potential return value
     */
    public static Closure async(Closure cl) {
        return {Object ... args -> callAsync(cl, *args)}
    }

    /**
     * Starts multiple closures in separate threads, collecting their return values
     * If an exception is thrown from the closure when called on any of the collection's elements,
     * it will be rethrown in the calling thread when it calls the Future.get() method.
     * @throws AsyncException If any of the collection's elements causes the closure to throw an exception. The original exceptions will be stored in the AsyncException's concurrentExceptions field.
     */
    public static List<Object> doInParallel(Closure ... closures) {
        return processResult(executeInParallel(closures))
    }

    /**
     * Starts multiple closures in separate threads, collecting Futures for their return values
     * If an exception is thrown from the closure when called on any of the collection's elements,
     * it will be rethrown in the calling thread when it calls the Future.get() method.
     */
    public static List<Future<Object>> executeInParallel(Closure ... closures) {
        Asynchronizer.withAsynchronizer(closures.size()) {ExecutorService executorService ->
            List<Future<Object>> result = closures.collect {cl ->
                return executorService.submit({
                    return cl.call()
                } as Callable<Object>)
            }
            return result
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
     * @return The thread that submits the closures to the thread executor service so that the caller can take ownership of it and e.g. call <i>join()</i> on it to wait for all the closures to finish processing.
     */
    public static Thread startInParallel(java.lang.Thread.UncaughtExceptionHandler uncaughtExceptionHandler, Closure ... closures) {
        final Thread thread = new Thread({
            doInParallel(closures)
        } as Runnable)
        thread.daemon = false
        thread.uncaughtExceptionHandler = uncaughtExceptionHandler
        thread.start()
        return thread
    }

    /**
     * Iterates over a collection/object with the <i>each()</i> method using an asynchronous variant of the supplied closure
     * to evaluate each collection's element. A CountDownLatch is used to make the calling thread wait for all the results.
     * After this method returns, all the closures have been finished and all the potential shared resources have been updated
     * by the threads.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Example:
     *      Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     *          def result = Collections.synchronizedSet(new HashSet())
     *          service.eachAsync([1, 2, 3, 4, 5]) {Number number -> result.add(number * 10)}*          assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     *}* Note that the <i>result</i> variable is synchronized to prevent race conditions between multiple threads.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withAsynchronizer</i> block
     * have a new <i>eachAsync(Closure cl)</i> method, which delegates to the <i>AsyncInvokerUtil</i> class.
     *    Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     *         def result = Collections.synchronizedSet(new HashSet())
     *        [1, 2, 3, 4, 5].eachAsync { Number number -> result.add(number * 10) }*         assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     *}* @throws AsyncException If any of the collection's elements causes the closure to throw an exception. The original exceptions will be stored in the AsyncException's concurrentExceptions field.
     */
    public static def eachAsync(Object collection, Closure cl) {
        final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<Throwable>())
        final Semaphore semaphore = new Semaphore(0)

        Closure code = async({Object ... args ->
            try {
                cl(*args)
            } catch (Throwable e) {
                exceptions.add(e)
            } finally {
                semaphore.release()
            }
        })
        int count = 0
        for(element in collection) {
            count += 1
            code.call(element)
        }
        semaphore.acquire(count)
        if (!exceptions.empty) throw new AsyncException("Some asynchronous operations failed. ${exceptions}", exceptions)
        else return collection
    }

    /**
     * Iterates over a collection/object with the <i>collect()</i> method using an asynchronous variant of the supplied closure
     * to evaluate each collection's element.
     * After this method returns, all the closures have been finished and the caller can safely use the result.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     *     Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     *         def result = service.collectAsync([1, 2, 3, 4, 5]){Number number -> number * 10}*         assertEquals(new HashSet([10, 20, 30, 40, 50]), new HashSet((Collection)result))
     *}*
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withAsynchronizer</i> block
     * have a new <i>collectAsync(Closure cl)</i> method, which delegates to the <i>AsyncInvokerUtil</i> class.
     *     Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     *         def result = [1, 2, 3, 4, 5].collectAsync{Number number -> number * 10}*         assertEquals(new HashSet([10, 20, 30, 40, 50]), new HashSet((Collection)result))
     *}* @throws AsyncException If any of the collection's elements causes the closure to throw an exception. The original exceptions will be stored in the AsyncException's concurrentExceptions field.
     */
    public static def collectAsync(Object collection, Closure cl) {
        return processResult(collection.collect(async(cl)))
    }

    /**
     * Performs the <i>findAll()</i> operation using an asynchronous variant of the supplied closure
     * to evaluate each collection's/object's element.
     * After this method returns, all the closures have been finished and the caller can safely use the result.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     *     def result = service.findAllAsync([1, 2, 3, 4, 5]){Number number -> number > 2}*     assertEquals(new HashSet([3, 4, 5]), new HashSet((Collection)result))
     *}*
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withAsynchronizer</i> block
     * have a new <i>findAllAsync(Closure cl)</i> method, which delegates to the <i>AsyncInvokerUtil</i> class.
     * Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     *     def result = [1, 2, 3, 4, 5].findAllAsync{Number number -> number > 2}*     assertEquals(new HashSet([3, 4, 5]), new HashSet((Collection)result))
     *}* @throws AsyncException If any of the collection's elements causes the closure to throw an exception. The original exceptions will be stored in the AsyncException's concurrentExceptions field.
     */
    public static def findAllAsync(Object collection, Closure cl) {
        collectAsync(collection, {if (cl(it)) return it else return null}).findAll {it != null}
    }

    /**
     * Performs the <i>find()</i> operation using an asynchronous variant of the supplied closure
     * to evaluate each collection's/object's element.
     * After this method returns, all the closures have been finished and the caller can safely use the result.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     *     def result = service.findAsync([1, 2, 3, 4, 5]){Number number -> number > 2}*     assert result in [3, 4, 5]
     *}*
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withAsynchronizer</i> block
     * have a new <i>findAllAsync(Closure cl)</i> method, which delegates to the <i>AsyncInvokerUtil</i> class.
     * Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     *     def result = [1, 2, 3, 4, 5].findAsync{Number number -> number > 2}*     assert result in [3, 4, 5]
     *}* @throws AsyncException If any of the collection's elements causes the closure to throw an exception. The original exceptions will be stored in the AsyncException's concurrentExceptions field.
     */
    public static def findAsync(Object collection, Closure cl) {
        collectAsync(collection, {if (cl(it)) return it else return null}).find {it != null}
    }

    /**
     * Performs the <i>all()</i> operation using an asynchronous variant of the supplied closure
     * to evaluate each collection's/object's element.
     * After this method returns, all the closures have been finished and the caller can safely use the result.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     *     assert service.allAsync([1, 2, 3, 4, 5]){Number number -> number > 0}*     assert !service.allAsync([1, 2, 3, 4, 5]){Number number -> number > 2}*}*
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withAsynchronizer</i> block
     * have a new <i>findAllAsync(Closure cl)</i> method, which delegates to the <i>AsyncInvokerUtil</i> class.
     * Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     *     assert [1, 2, 3, 4, 5].allAsync{Number number -> number > 0}*     assert ![1, 2, 3, 4, 5].allAsync{Number number -> number > 2}*}* @throws AsyncException If any of the collection's elements causes the closure to throw an exception. The original exceptions will be stored in the AsyncException's concurrentExceptions field.
     */
    public static boolean allAsync(Object collection, Closure cl) {
        final AtomicBoolean flag = new AtomicBoolean(true)
        eachAsync(collection, {value -> if (!cl(value)) flag.set(false)})
        return flag.get()
    }

    /**
     * Performs the <i>any()</i> operation using an asynchronous variant of the supplied closure
     * to evaluate each collection's/object's element.
     * After this method returns, all the closures have been finished and the caller can safely use the result.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     *     assert service.anyAsync([1, 2, 3, 4, 5]){Number number -> number > 2}*     assert !service.anyAsync([1, 2, 3, 4, 5]){Number number -> number > 6}*}*
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withAsynchronizer</i> block
     * have a new <i>findAllAsync(Closure cl)</i> method, which delegates to the <i>AsyncInvokerUtil</i> class.
     * Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     *     assert [1, 2, 3, 4, 5].anyAsync{Number number -> number > 2}*     assert ![1, 2, 3, 4, 5].anyAsync{Number number -> number > 6}*}* @throws AsyncException If any of the collection's elements causes the closure to throw an exception. The original exceptions will be stored in the AsyncException's concurrentExceptions field.
     */
    public static boolean anyAsync(Object collection, Closure cl) {
        final AtomicBoolean flag = new AtomicBoolean(false)
        eachAsync(collection, {if (cl(it)) flag.set(true)})
        return flag.get()
    }

    private static List<Object> processResult(List<Future<Object>> futures) {
        final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<Throwable>())

        final List<Object> result = futures.collect {
            try {
                return it.get()
            } catch (Throwable e) {
                exceptions.add(e)
                return e
            }
        }

        if (!exceptions.empty) throw new AsyncException("Some asynchronous operations failed. ${exceptions}", exceptions)
        else return result
    }

    private static UncaughtExceptionHandler createDefaultUncaughtExceptionHandler() {
        return {Thread failedThread, Throwable throwable ->
            System.err.println "Error processing background thread ${failedThread.name}: ${throwable.message}"
            throwable.printStackTrace(System.err)
        } as UncaughtExceptionHandler
    }
}
