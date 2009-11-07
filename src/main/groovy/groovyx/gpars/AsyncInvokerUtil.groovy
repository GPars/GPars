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

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.*

/**
 * This class forms the core of the DSL initialized by <i>Asynchronizer</i>. The static methods of <i>AsyncInvokerUtil</i>
 * get attached to their first arguments (the Groovy Category mechanism) and can be then invoked as if they were part of
 * the argument classes.
 * @see groovyx.gpars.Asynchronizer
 *
 * @author Vaclav Pech
 * Date: Oct 23, 2008
 */
public class AsyncInvokerUtil {
    /**
     * schedules the supplied closure for processing in the underlying thread pool.
     */
    private static Future callParallel(Closure task) {
        final ExecutorService pool = Asynchronizer.retrieveCurrentPool()
        if (!pool) throw new IllegalStateException("No ExecutorService available for the current thread.")
        return pool.submit(task as Callable)
    }

    /**
     * Calls a closure in a separate thread supplying the given arguments, returning a future for the potential return value,
     */
    public static Future callAsync(final Closure cl, final Object ... args) {
        callParallel {-> cl(* args)}
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
        return {Object ... args -> callAsync(cl, * args)}
    }

    /**
     * Iterates over a collection/object with the <i>each()</i> method using an asynchronous variant of the supplied closure
     * to evaluate each collection's element. A Semaphore is used to make the calling thread wait for all the results.
     * After this method returns, all the closures have been finished and all the potential shared resources have been updated
     * by the threads.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Example:
     *      Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     *          def result = Collections.synchronizedSet(new HashSet())
     *          service.eachParallel([1, 2, 3, 4, 5]) {Number number -> result.add(number * 10)}*          assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     *}* Note that the <i>result</i> variable is synchronized to prevent race conditions between multiple threads.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withAsynchronizer</i> block
     * have a new <i>eachParallel(Closure cl)</i> method, which delegates to the <i>AsyncInvokerUtil</i> class.
     *    Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     *         def result = Collections.synchronizedSet(new HashSet())
     *        [1, 2, 3, 4, 5].eachParallel { Number number -> result.add(number * 10) }*         assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     *}* @throws AsyncException If any of the collection's elements causes the closure to throw an exception. The original exceptions will be stored in the AsyncException's concurrentExceptions field.
     */
    public static def eachParallel(Object collection, Closure cl) {
        final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<Throwable>())
        final Semaphore semaphore = new Semaphore(0)
        Closure code = async({Object ... args ->
            try {
                cl(* args)
            } catch (Throwable e) {
                exceptions.add(e)
            } finally {
                semaphore.release()
            }
        })
        int count = 0
        for (element in collection) {
            count += 1
            code.call(element)
        }
        semaphore.acquire(count)
        if (exceptions.empty) return collection
        else throw new AsyncException("Some asynchronous operations failed. ${exceptions}", exceptions)
    }

    /**
     * Iterates over a collection/object with the <i>eachWithIndex()</i> method using an asynchronous variant of the supplied closure
     * to evaluate each collection's element. A Semaphore is used to make the calling thread wait for all the results.
     * After this method returns, all the closures have been finished and all the potential shared resources have been updated
     * by the threads.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Example:
     *      Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     *          def result = Collections.synchronizedSet(new HashSet())
     *          service.eachWithIndexParallel([1, 2, 3, 4, 5]) {Number number -> result.add(number * 10)}*          assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     *}* Note that the <i>result</i> variable is synchronized to prevent race conditions between multiple threads.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withAsynchronizer</i> block
     * have a new <i>eachParallel(Closure cl)</i> method, which delegates to the <i>AsyncInvokerUtil</i> class.
     *    Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     *         def result = Collections.synchronizedSet(new HashSet())
     *        [1, 2, 3, 4, 5].eachWithIndexParallel { Number number, int index -> result.add(number * 10) }*         assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     *}* @throws AsyncException If any of the collection's elements causes the closure to throw an exception. The original exceptions will be stored in the AsyncException's concurrentExceptions field.
     */
    public static def eachWithIndexParallel(Object collection, Closure cl) {
        final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<Throwable>())
        final Semaphore semaphore = new Semaphore(0)
        Closure code = async({Object element, int index ->
            try {
                cl(element, index)
            } catch (Throwable e) {
                exceptions.add(e)
            } finally {
                semaphore.release()
            }
        })
        int count = 0
        for (element in collection) {
            code.call(element, count)
            count += 1
        }
        semaphore.acquire(count)
        if (exceptions.empty) return collection
        else throw new AsyncException("Some asynchronous operations failed. ${exceptions}", exceptions)
    }

    /**
     * Iterates over a collection/object with the <i>collect()</i> method using an asynchronous variant of the supplied closure
     * to evaluate each collection's element.
     * After this method returns, all the closures have been finished and the caller can safely use the result.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     *     Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     *         def result = service.collectParallel([1, 2, 3, 4, 5]){Number number -> number * 10}*         assertEquals(new HashSet([10, 20, 30, 40, 50]), new HashSet((Collection)result))
     *}*
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withAsynchronizer</i> block
     * have a new <i>collectParallel(Closure cl)</i> method, which delegates to the <i>AsyncInvokerUtil</i> class.
     *     Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     *         def result = [1, 2, 3, 4, 5].collectParallel{Number number -> number * 10}*         assertEquals(new HashSet([10, 20, 30, 40, 50]), new HashSet((Collection)result))
     *}* @throws AsyncException If any of the collection's elements causes the closure to throw an exception. The original exceptions will be stored in the AsyncException's concurrentExceptions field.
     */
    public static def collectParallel(Object collection, Closure cl) {
        return processResult(collection.collect(async(cl)))
    }

    /**
     * Performs the <i>findAll()</i> operation using an asynchronous variant of the supplied closure
     * to evaluate each collection's/object's element.
     * After this method returns, all the closures have been finished and the caller can safely use the result.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     *     def result = service.findAllParallel([1, 2, 3, 4, 5]){Number number -> number > 2}*     assertEquals(new HashSet([3, 4, 5]), new HashSet((Collection)result))
     *}*
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withAsynchronizer</i> block
     * have a new <i>findAllParallel(Closure cl)</i> method, which delegates to the <i>AsyncInvokerUtil</i> class.
     * Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     *     def result = [1, 2, 3, 4, 5].findAllParallel{Number number -> number > 2}*     assertEquals(new HashSet([3, 4, 5]), new HashSet((Collection)result))
     *}* @throws AsyncException If any of the collection's elements causes the closure to throw an exception. The original exceptions will be stored in the AsyncException's concurrentExceptions field.
     */
    public static def findAllParallel(Object collection, Closure cl) {
        collectParallel(collection, {if (cl(it)) return it else return null}).findAll {it != null}
    }

    /**
     * Performs the <i>grep()()</i> operation using an asynchronous variant of the supplied closure
     * to evaluate each collection's/object's element.
     * After this method returns, all the closures have been finished and the caller can safely use the result.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     *     def result = service.grepParallel([1, 2, 3, 4, 5])(3..6)
     *     assertEquals(new HashSet([3, 4, 5]), new HashSet((Collection)result))
     *}*
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withAsynchronizer</i> block
     * have a new <i>findAllParallel(Closure cl)</i> method, which delegates to the <i>AsyncInvokerUtil</i> class.
     * Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     *     def result = [1, 2, 3, 4, 5].grepParallel(3..6)
     *     assertEquals(new HashSet([3, 4, 5]), new HashSet((Collection)result))
     *}* @throws AsyncException If any of the collection's elements causes the closure to throw an exception. The original exceptions will be stored in the AsyncException's concurrentExceptions field.
     */
    public static def grepParallel(Object collection, filter) {
        collectParallel(collection, {if (filter.isCase(it)) return it else return null}).findAll {it != null}
    }

    /**
     * Performs the <i>find()</i> operation using an asynchronous variant of the supplied closure
     * to evaluate each collection's/object's element.
     * After this method returns, all the closures have been finished and the caller can safely use the result.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     *     def result = service.findParallel([1, 2, 3, 4, 5]){Number number -> number > 2}*     assert result in [3, 4, 5]
     *}*
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withAsynchronizer</i> block
     * have a new <i>findAllParallel(Closure cl)</i> method, which delegates to the <i>AsyncInvokerUtil</i> class.
     * Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     *     def result = [1, 2, 3, 4, 5].findParallel{Number number -> number > 2}*     assert result in [3, 4, 5]
     *}* @throws AsyncException If any of the collection's elements causes the closure to throw an exception. The original exceptions will be stored in the AsyncException's concurrentExceptions field.
     */
    public static def findParallel(Object collection, Closure cl) {
        collectParallel(collection, {if (cl(it)) return it else return null}).find {it != null}
    }

    /**
     * Performs the <i>all()</i> operation using an asynchronous variant of the supplied closure
     * to evaluate each collection's/object's element.
     * After this method returns, all the closures have been finished and the caller can safely use the result.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     *     assert service.everyParallel([1, 2, 3, 4, 5]){Number number -> number > 0}*     assert !service.everyParallel([1, 2, 3, 4, 5]){Number number -> number > 2}*}*
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withAsynchronizer</i> block
     * have a new <i>findAllParallel(Closure cl)</i> method, which delegates to the <i>AsyncInvokerUtil</i> class.
     * Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     *     assert [1, 2, 3, 4, 5].everyParallel{Number number -> number > 0}*     assert ![1, 2, 3, 4, 5].everyParallel{Number number -> number > 2}*}* @throws AsyncException If any of the collection's elements causes the closure to throw an exception. The original exceptions will be stored in the AsyncException's concurrentExceptions field.
     */
    public static boolean everyParallel(Object collection, Closure cl) {
        final AtomicBoolean flag = new AtomicBoolean(true)
        eachParallel(collection, {value -> if (!cl(value)) flag.set(false)})
        return flag.get()
    }

    /**
     * Performs the <i>any()</i> operation using an asynchronous variant of the supplied closure
     * to evaluate each collection's/object's element.
     * After this method returns, all the closures have been finished and the caller can safely use the result.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     *     assert service.anyParallel([1, 2, 3, 4, 5]){Number number -> number > 2}*     assert !service.anyParallel([1, 2, 3, 4, 5]){Number number -> number > 6}*}*
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withAsynchronizer</i> block
     * have a new <i>anyParallel(Closure cl)</i> method, which delegates to the <i>AsyncInvokerUtil</i> class.
     * Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     *     assert [1, 2, 3, 4, 5].anyParallel{Number number -> number > 2}*     assert ![1, 2, 3, 4, 5].anyParallel{Number number -> number > 6}*}* @throws AsyncException If any of the collection's elements causes the closure to throw an exception. The original exceptions will be stored in the AsyncException's concurrentExceptions field.
     */
    public static boolean anyParallel(Object collection, Closure cl) {
        final AtomicBoolean flag = new AtomicBoolean(false)
        eachParallel(collection, {if (cl(it)) flag.set(true)})
        return flag.get()
    }

    /**
     * Performs the <i>groupBy()</i> operation using an asynchronous variant of the supplied closure
     * to evaluate each collection's/object's element.
     * After this method returns, all the closures have been finished and the caller can safely use the result.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     *     assert service.groupByParallel(([1, 2, 3, 4, 5]){Number number -> number % 2}).size() == 2
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withAsynchronizer</i> block
     * have a new <i>groupByParallel(Closure cl)</i> method, which delegates to the <i>AsyncInvokerUtil</i> class.
     * Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
     *     assert ([1, 2, 3, 4, 5].groupByParallel{Number number -> number % 2}).size() == 2
     */
    public static Map groupByParallel(Object collection, Closure cl) {
        final def map = new ConcurrentHashMap()
        eachParallel(collection, {
            def result = cl(it)
            final def myList = [it].asSynchronized()
            def list = map.putIfAbsent(result, myList)
            if (list != null) list.add(it)
        })
        return map
    }

    static List<Object> processResult(List<Future<Object>> futures) {
        final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<Throwable>())

        final List<Object> result = futures.collect {
            try {
                return it.get()
            } catch (Throwable e) {
                exceptions.add(e)
                return e
            }
        }

        if (exceptions.empty) return result
        else throw new AsyncException("Some asynchronous operations failed. ${exceptions}", exceptions)
    }
}
