// GPars - Groovy Parallel Systems
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

import groovy.time.Duration
import groovyx.gpars.memoize.LRUProtectionStorage
import groovyx.gpars.memoize.NullProtectionStorage
import groovyx.gpars.memoize.NullValue
import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import jsr166y.forkjoin.ForkJoinExecutor
import jsr166y.forkjoin.ForkJoinPool
import jsr166y.forkjoin.Ops.Mapper
import jsr166y.forkjoin.Ops.Predicate
import jsr166y.forkjoin.Ops.Reducer
import jsr166y.forkjoin.ParallelArray
import jsr166y.forkjoin.RecursiveTask

/**
 * This class forms the core of the DSL initialized by <i>GParsPool</i>. The static methods of <i>GParsPoolUtil</i>
 * get attached to their first arguments (the Groovy Category mechanism) and can be then invoked as if they were part of
 * the argument classes.
 * @see groovyx.gpars.GParsPool
 *
 * @author Vaclav Pech
 * @author Robert Fischer
 * Date: Mar 10, 2010
 */
public class GParsPoolUtil {

    final static String MEMOIZE_NULL = new NullValue()

    /**
     * Allows timeouts for async operations
     */
    private static final Timer timer = new Timer('GParsExecutorsTimeoutTimer', true)

    private static ForkJoinPool retrievePool() {
        final ForkJoinPool pool = groovyx.gpars.GParsPool.retrieveCurrentPool()
        if (pool == null) throw new IllegalStateException("No ForkJoinPool available for the current thread")
        return pool
    }

    /**
     * schedules the supplied closure for processing in the underlying thread pool.
     */
    private static Future callParallel(Closure task) {
        final ForkJoinPool pool = groovyx.gpars.GParsPool.retrieveCurrentPool()
        if (!pool) throw new IllegalStateException("No ExecutorService available for the current thread.")
        return pool.submit([compute: task] as RecursiveTask)
    }

    /**
     * Calls a closure in a separate thread supplying the given arguments, returning a future for the potential return value.
     */
    public static Future callAsync(final Closure cl, final Object... args) {
        callParallel {-> cl(* args)}
    }

    /**
     * Calls a closure in a separate thread supplying the given arguments, returning a future for the potential return value.
     * Also allows the asynchronous calculation to be cancelled after a given timeout.
     * In order to allow cancellation, the asynchronously running code must keep checking the _interrupted_ flag of its
     * own thread and cease the calculation once the flag is set to true.
     * @param timeout The timeout in milliseconds to wait before the calculation gets cancelled.
     */
    public static Future callTimeoutAsync(final Closure cl, long timeout, final Object... args) {
        final Future f = callAsync(cl, args)
        timer.schedule({f.cancel(true)} as TimerTask, timeout)
        return f
    }

    /**
     * Calls a closure in a separate thread supplying the given arguments, returning a future for the potential return value.
     * Also allows the asynchronous calculation to be cancelled after a given timeout.
     * In order to allow cancellation, the asynchronously running code must keep checking the _interrupted_ flag of its
     * own thread and cease the calculation once the flag is set to true.
     * @param timeout The timeout to wait before the calculation gets cancelled.
     */
    public static Future callTimeoutAsync(final Closure cl, Duration timeout, final Object... args) {
        callTimeoutAsync(cl, timeout.toMilliseconds(), args)
    }

    /**
     * Submits the task for asynchronous processing returning the Future received from the executor service.
     * Allows for the following syntax:
     * <pre>
     * executorService << {println 'Inside parallel task'}* </pre>
     */
    public static Future leftShift(ForkJoinPool pool, Closure task) {
        return pool.submit([compute: task] as RecursiveTask)
    }

    /**
     * Creates an asynchronous variant of the supplied closure, which, when invoked returns a future for the potential return value
     */
    public static Closure async(Closure cl) {
        return {Object... args -> callAsync(cl, * args)}
    }

    /**
     * Creates a caching variant of the supplied closure.
     * Whenever the closure is called, the mapping between the parameters and the return value is preserved in cache
     * making subsequent calls with the same arguments fast.
     * This variant will keep all values forever.
     * The returned function can be safely used concurrently from multiple threads, however, the implementation
     * values high average-scenario performance and so concurrent calls on the memoized function with identical argument values
     * may not necessarily be able to benefit from each other's cached return value. With this having been mentioned,
     * the performance trade-off still makes concurrent use of memoized functions safe and highly recommended.
     */
    public static Closure memoize(Closure cl) {
        return buildMemoizeFunction([:] as ConcurrentHashMap, cl)
    }

    /**
     * Creates a caching variant of the supplied closure with upper limit on the cache size.
     * Whenever the closure is called, the mapping between the parameters and the return value is preserved in cache
     * making subsequent calls with the same arguments fast.
     * This variant will keep all values until the upper size limit is reached. Then the values in the cache start rotating
     * using the LRU (Last Recently Used) strategy.
     * The returned function can be safely used concurrently from multiple threads, however, the implementation
     * values high average-scenario performance and so concurrent calls on the memoized function with identical argument values
     * may not necessarily be able to benefit from each other's cached return value. With this having been mentioned,
     * the performance trade-off still makes concurrent use of memoized functions safe and highly recommended.
     */
    public static Closure memoizeAtMost(Closure cl, int maxCacheSize) {
        if (maxCacheSize < 0) throw new IllegalArgumentException("A non-negative number is required as the maxCacheSize parameter for memoizeAtMost.")

        return buildMemoizeFunction(new LRUProtectionStorage(maxCacheSize).asSynchronized(), cl)
    }

    private static def buildMemoizeFunction(cache, Closure cl) {
        return {Object... args ->
            def key = args.collect {it}
            Object result = cache[key]
            if (result == null) {
                result = cl.call(* args)
                //noinspection GroovyConditionalCanBeElvis
                cache[key] = result != null ? result : MEMOIZE_NULL
            }
            result == MEMOIZE_NULL ? null : result
        }
    }

    /**
     * Creates a caching variant of the supplied closure with automatic cache size adjustment and lower limit
     * on the cache size.
     * Whenever the closure is called, the mapping between the parameters and the return value is preserved in cache
     * making subsequent calls with the same arguments fast.
     * This variant allows the garbage collector to release entries from the cache and at the same time allows
     * the user to specify how many entries should be protected from the eventual gc-initiated eviction.
     * Cached entries exceeding the specified preservation threshold are made available for eviction based on
     * the LRU (Last Recently Used) strategy.
     * Given the non-deterministic nature of garbage collector, the actual cache size may grow well beyond the limits
     * set by the user if memory is plentiful.
     * The returned function can be safely used concurrently from multiple threads, however, the implementation
     * values high average-scenario performance and so concurrent calls on the memoized function with identical argument values
     * may not necessarily be able to benefit from each other's cached return value. Also the protectedCacheSize parameter
     * might not be respected accurately in such scenarios for some periods of time. With this having been mentioned,
     * the performance trade-off still makes concurrent use of memoized functions safe and highly recommended.
     */
    public static Closure memoizeAtLeast(Closure cl, int protectedCacheSize) {
        if (protectedCacheSize < 0) throw new IllegalArgumentException("A non-negative number is required as the protectedCacheSize parameter for memoizeAtLeast.")

        return buildSoftReferenceMemoizeFunction(protectedCacheSize, [:] as ConcurrentHashMap, cl)
    }

    /**
     * Creates a caching variant of the supplied closure with automatic cache size adjustment and lower and upper limits
     * on the cache size.
     * Whenever the closure is called, the mapping between the parameters and the return value is preserved in cache
     * making subsequent calls with the same arguments fast.
     * This variant allows the garbage collector to release entries from the cache and at the same time allows
     * the user to specify how many entries should be protected from the eventual gc-initiated eviction.
     * Cached entries exceeding the specified preservation threshold are made available for eviction based on
     * the LRU (Last Recently Used) strategy.
     * Given the non-deterministic nature of garbage collector, the actual cache size may grow well beyond the protected
     * size limits set by the user, if memory is plentiful.
     * Also, this variant will never exceed in size the upper size limit. Once the upper size limit has been reached,
     * the values in the cache start rotating using the LRU (Last Recently Used) strategy.
     * The returned function can be safely used concurrently from multiple threads, however, the implementation
     * values high average-scenario performance and so concurrent calls on the memoized function with identical argument values
     * may not necessarily be able to benefit from each other's cached return value. Also the protectedCacheSize parameter
     * might not be respected accurately in such scenarios for some periods of time. With this having been mentioned,
     * the performance trade-off still makes concurrent use of memoized functions safe and highly recommended.
     */
    public static Closure memoizeBetween(Closure cl, int protectedCacheSize, int maxCacheSize) {
        if (protectedCacheSize < 0) throw new IllegalArgumentException("A non-negative number is required as the protectedCacheSize parameter for memoizeBetween.")
        if (maxCacheSize < 0) throw new IllegalArgumentException("A non-negative number is required as the maxCacheSize parameter for memoizeBetween.")
        if (protectedCacheSize > maxCacheSize) throw new IllegalArgumentException("The maxCacheSize parameter to memoizeBetween is required to be greater or equal to the protectedCacheSize parameter.")

        return buildSoftReferenceMemoizeFunction(protectedCacheSize, new LRUProtectionStorage(maxCacheSize).asSynchronized(), cl)
    }

    private static def buildSoftReferenceMemoizeFunction(int protectedCacheSize, cache, Closure cl) {
        def lruProtectionStorage = protectedCacheSize > 0 ?
            new LRUProtectionStorage(protectedCacheSize) :
            new NullProtectionStorage() //Nothing should be done when no elements need protection against eviction

        return {Object... args ->
            cleanUpNullReferences(cache)
            def key = args.collect {it}
            def result = cache[key]?.get()
            if (result == null) {
                result = cl.call(* args)
                if (result == null) {
                    result = new NullValue()
                }
                cache[key] = new SoftReference(result)
            }
            lruProtectionStorage.touch(key, result)
            result == MEMOIZE_NULL ? null : result
        }
    }

    private static void cleanUpNullReferences(cache) {
        cache.findAllParallel({entry -> entry.value.get() == null}).eachParallel {entry -> cache.remove entry.key}
    }

    private static <T> ParallelArray<T> createPA(Collection<T> collection, ForkJoinExecutor pool) {
        return ParallelArray.createFromCopy(collection.toArray(new T[collection.size()]), pool)
    }

    static java.util.Collection createCollection(Object object) {
        def collection = []
        for (element in object) collection << element
        return collection
    }

    /**
     * Creates a TransparentParallel class instance and mixes it in the object it is invoked on. The TransparentParallel class
     * overrides the iterative methods like each(), collect() and such, so that they call their parallel variants from the GParsPoolUtil class
     * like eachParallel(), collectParallel() and such.
     * After mixing-in, the isTransparent() method will return true.
     * @param collection The object to make transparent
     * @return The instance of the TransparentParallel class wrapping the original object and overriding the iterative methods with new parallel behavior
     */
    public static Object makeTransparent(Object collection) {
        if (!(collection.respondsTo('isTransparent'))) throw new IllegalStateException("Cannot make the object transparently parallel. Apparently we're not inside a GParsPool.withPool() block nor the collection hasn't been enhanced with ParallelEnhancer.enhance().")
        if (!collection.isTransparent()) collection.metaClass.mixin(TransparentParallel)
        return collection
    }

    /**
     * Indicates whether the iterative methods like each() or collect() work have been altered to work concurrently.
     */
    public static boolean isTransparent(Object collection) { false }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withMapping() method using the supplied
     * closure as the transformation operation.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>eachParallel(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * GParsPool.withPool {*     def result = new ConcurrentSkipListSet()
     *     [1, 2, 3, 4, 5].eachParallel {Number number -> result.add(number * 10)}*     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     *}* Note that the <i>result</i> variable is synchronized to prevent race conditions between multiple threads.
     */
    public static <T> Collection<T> eachParallel(Collection<T> collection, Closure cl) {
        createPA(collection, retrievePool()).withMapping({cl(it)} as Mapper).all()
        return collection
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withMapping() method using the supplied
     * closure as the transformation operation.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>eachParallel(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * GParsPool.withPool {*     def result = new ConcurrentSkipListSet()
     *     [1, 2, 3, 4, 5].eachParallel {Number number -> result.add(number * 10)}*     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     *}* Note that the <i>result</i> variable is synchronized to prevent race conditions between multiple threads.
     */
    public static Object eachParallel(Object collection, Closure cl) {
        eachParallel(createCollection(collection), cl)
        return collection
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withMapping() method using the supplied
     * closure as the transformation operation.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>eachWithIndexParallel(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * GParsPool.withPool {*     def result = new ConcurrentSkipListSet()
     *     [1, 2, 3, 4, 5].eachWithIndexParallel {Number number, int index -> result.add(number * 10)}*     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     *}* Note that the <i>result</i> variable is synchronized to prevent race conditions between multiple threads.
     */
    public static <T> Collection<T> eachWithIndexParallel(Collection<T> collection, Closure cl) {
        def indexedCollection = []
        int index = 0
        for (element in collection) {
            indexedCollection << [element, index]
            index++
        }
        createPA(indexedCollection, retrievePool()).withMapping({cl(it[0], it[1])} as Mapper).all()
        return collection
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withMapping() method using the supplied
     * closure as the transformation operation.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>eachWithIndexParallel(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * GParsPool.withPool {*     def result = new ConcurrentSkipListSet()
     *     [1, 2, 3, 4, 5].eachWithIndexParallel {Number number, int index -> result.add(number * 10)}*     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     *}* Note that the <i>result</i> variable is synchronized to prevent race conditions between multiple threads.
     */
    public static Object eachWithIndexParallel(Object collection, Closure cl) {
        eachWithIndexParallel(createCollection(collection), cl)
        return collection
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withMapping() method using the supplied
     * closure as the transformation operation.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a collection of values from the resulting Parallel Array.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>collectParallel(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].collectParallel {Number number -> number * 10}*     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     *}*/
    public static <T> Collection<T> collectParallel(Collection<T> collection, Closure cl) {
        createPA(collection, retrievePool()).withMapping({cl(it)} as Mapper).all().asList()
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withMapping() method using the supplied
     * closure as the transformation operation.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a collection of values from the resulting Parallel Array.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>collectParallel(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].collectParallel {Number number -> number * 10}*     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     *}*/
    public static Collection<Object> collectParallel(Object collection, Closure cl) {
        return collectParallel(createCollection(collection), cl)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withFilter() method using the supplied
     * closure as the filter predicate.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a collection of values from the resulting Parallel Array.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>findAllParallel(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].findAllParallel {Number number -> number > 3}*     assertEquals(new HashSet([4, 5]), result)
     *}*/
    public static <T> Collection<T> findAllParallel(Collection<T> collection, Closure cl) {
        createPA(collection, retrievePool()).withFilter({cl(it) as Boolean} as Predicate).all().asList()
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withFilter() method using the supplied
     * closure as the filter predicate.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a collection of values from the resulting Parallel Array.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>findAllParallel(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].findAllParallel {Number number -> number > 3}*     assertEquals(new HashSet([4, 5]), result)
     *}*/
    public static Collection<Object> findAllParallel(Object collection, Closure cl) {
        return findAllParallel(createCollection(collection), cl)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withFilter() method using the supplied
     * closure as the filter predicate.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a value from the resulting Parallel Array with the minimum index.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>findParallel(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].findParallel {Number number -> number > 3}*     assert (result in [4, 5])
     *}*/
    public static <T> Object findParallel(Collection<T> collection, Closure cl) {
        final ParallelArray found = createPA(collection, retrievePool()).withFilter({cl(it) as Boolean} as Predicate).all()
        if (found.size() > 0) found.get(0)
        else return null
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withFilter() method using the supplied
     * closure as the filter predicate.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a value from the resulting Parallel Array with the minimum index.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>findParallel(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].findParallel {Number number -> number > 3}*     assert (result in [4, 5])
     *}*/
    public static Object findParallel(Object collection, Closure cl) {
        return findParallel(createCollection(collection), cl)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withFilter() method using the supplied
     * closure as the filter predicate.
     * Unlike with the <i>find</i> method, findAnyParallel() does not guarantee
     * that the a matching element with the lowest index is returned.
     * The findAnyParallel() method evaluates elements lazily and stops processing further elements of the collection once a match has been found.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a random value from the resulting Parallel Array.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>findParallel(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].findParallel {Number number -> number > 3}*     assert (result in [4, 5])
     *}*/
    public static <T> Object findAnyParallel(Collection<T> collection, Closure cl) {
        createPA(collection, retrievePool()).withFilter({cl(it) as Boolean} as Predicate).any()
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withFilter() method using the supplied
     * closure as the filter predicate.
     * Unlike with the <i>find</i> method, findAnyParallel() does not guarantee
     * that the a matching element with the lowest index is returned.
     * The findAnyParallel() method evaluates elements lazily and stops processing further elements of the collection once a match has been found.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a random value from the resulting Parallel Array.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>findParallel(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].findParallel {Number number -> number > 3}*     assert (result in [4, 5])
     *}*/
    public static Object findAnyParallel(Object collection, Closure cl) {
        return findParallel(createCollection(collection), cl)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withFilter() method using the supplied
     * rule as the filter predicate.
     * The filter will be effectively used concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a collection of values from the resulting Parallel Array.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>grepParallel(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].grepParallel(4..6)
     *     assertEquals(new HashSet([4, 5]), result)
     *}*/
    public static <T> Collection<T> grepParallel(Collection<T> collection, filter) {
        createPA(collection, retrievePool()).withFilter({filter.isCase it} as Predicate).all().asList()
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withFilter() method using the supplied
     * rule as the filter predicate.
     * The filter will be effectively used concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a collection of values from the resulting Parallel Array.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>grepParallel(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].grepParallel(4..6)
     *     assertEquals(new HashSet([4, 5]), result)
     *}*/
    public static Object grepParallel(Object collection, filter) {
        return grepParallel(createCollection(collection), filter)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withFilter() method using the supplied
     * rule as the filter predicate.
     * The filter will be effectively used concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a collection of values from the resulting Parallel Array.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>grepParallel(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].splitParallel(it > 3)
     *            assert [3, 4, 5] as Set == result[0] as Set
     *            assert [1, 2] as Set == result[1] as Set

     *}*/
    public static <T> Collection<T> splitParallel(Collection<T> collection, filter) {
        final def groups = groupByParallel(collection, filter)
        return [groups[true] ?: [], groups[false] ?: []]
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withFilter() method using the supplied
     * rule as the filter predicate.
     * The filter will be effectively used concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a collection of values from the resulting Parallel Array.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>grepParallel(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].splitParallel(4..6)
     *            assert [3, 4, 5] as Set == result[0] as Set
     *            assert [1, 2] as Set == result[1] as Set
     *}*/
    public static Object splitParallel(Object collection, filter) {
        return splitParallel(createCollection(collection), filter)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withFilter() method using the supplied
     * rule as the filter predicate.
     * The filter will be effectively used concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a collection of values from the resulting Parallel Array.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>grepParallel(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].countParallel(4)
     *     assertEquals(1, result)
     *}*/
    public static <T> int countParallel(Collection<T> collection, filter) {
        createPA(collection, retrievePool()).withFilter({filter == it} as Predicate).size()
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withFilter() method using the supplied
     * rule as the filter predicate.
     * The filter will be effectively used concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a collection of values from the resulting Parallel Array.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>grepParallel(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].countParallel(4)
     *     assertEquals(1, result)
     *}*/
    public static int countParallel(Object collection, filter) {
        return countParallel(createCollection(collection), filter)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withFilter() method using the supplied
     * closure as the filter predicate.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * The anyParallel() method is lazy and once a positive answer has been given by at least one element, it avoids running
     * the supplied closure on subsequent elements.
     * After all the elements have been processed, the method returns a boolean value indicating, whether at least
     * one element of the collection meets the predicate.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>anyParallel(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * GParsPool.withPool {*     assert [1, 2, 3, 4, 5].anyParallel {Number number -> number > 3}*     assert ![1, 2, 3].anyParallel {Number number -> number > 3}*}*/
    public static <T> boolean anyParallel(Collection<T> collection, Closure cl) {
        createPA(collection, retrievePool()).withFilter({cl(it) as Boolean} as Predicate).any() != null
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withFilter() method using the supplied
     * closure as the filter predicate.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * The anyParallel() method is lazy and once a positive answer has been given by at least one element, it avoids running
     * the supplied closure on subsequent elements.
     * After all the elements have been processed, the method returns a boolean value indicating, whether at least
     * one element of the collection meets the predicate.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>anyParallel(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * GParsPool.withPool {*     assert [1, 2, 3, 4, 5].anyParallel {Number number -> number > 3}*     assert ![1, 2, 3].anyParallel {Number number -> number > 3}*}*/
    public static boolean anyParallel(Object collection, Closure cl) {
        return anyParallel(createCollection(collection), cl)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withFilter() method using the supplied
     * closure as the filter predicate.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a boolean value indicating, whether all the elements
     * of the collection meet the predicate.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>everyParallel(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * GParsPool.withPool(5) {*     assert ![1, 2, 3, 4, 5].everyParallel {Number number -> number > 3}*     assert [1, 2, 3].everyParallel() {Number number -> number <= 3}*}*/
    public static <T> boolean everyParallel(Collection<T> collection, Closure cl) {
        createPA(collection, retrievePool()).withFilter({cl(it) as Boolean} as Predicate).all().size() == collection.size()
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withFilter() method using the supplied
     * closure as the filter predicate.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a boolean value indicating, whether all the elements
     * of the collection meet the predicate.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>everyParallel(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * GParsPool.withPool(5) {*     assert ![1, 2, 3, 4, 5].everyParallel {Number number -> number > 3}*     assert [1, 2, 3].everyParallel() {Number number -> number <= 3}*}*/
    public static boolean everyParallel(Object collection, Closure cl) {
        return everyParallel(createCollection(collection), cl)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withMapping() method using the supplied
     * closure as the mapping predicate.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a list of groups of the original elements.
     * Elements in the same group gave identical results when the supplied closure was invoked on them.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>groupByParallel(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * GParsPool.withPool {*     assert ([1, 2, 3, 4, 5].groupByParallel {Number number -> number % 2}).size() == 2
     *}*/
    public static <T> Map groupByParallel(Collection<T> collection, Closure cl) {
        final def map = new ConcurrentHashMap()
        eachParallel(collection, {
            def result = cl(it)
            final def myList = [it].asSynchronized()
            def list = map.putIfAbsent(result, myList)
            if (list != null) list.add(it)
        })
        return map

    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withMapping() method using the supplied
     * closure as the mapping predicate.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a list of groups of the original elements.
     * Elements in the same group gave identical results when the supplied closure was invoked on them.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>groupByParallel(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * GParsPool.withPool {*     assert ([1, 2, 3, 4, 5].groupByParallel {Number number -> number % 2}).size() == 2
     *}*/
    public static Map groupByParallel(Object collection, Closure cl) {
        return groupByParallel(createCollection(collection), cl)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its min() method using the supplied
     * closure as the comparator.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the minimum of the elements in the collection.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * If the supplied closure takes two arguments it is used directly as a comparator.
     * If the supplied closure takes one argument, the values returned by the supplied closure for individual elements are used for comparison by the implicit comparator.
     * @param cl A one or two-argument closure
     */
    public static <T> T minParallel(Collection<T> collection, Closure cl) {
        createPA(collection, retrievePool()).min(createComparator(cl) as Comparator)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its min() method using the supplied
     * closure as the comparator.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the minimum of the elements in the collection.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * If the supplied closure takes two arguments it is used directly as a comparator.
     * If the supplied closure takes one argument, the values returned by the supplied closure for individual elements are used for comparison by the implicit comparator.
     * @param cl A one or two-argument closure
     */
    public static Object minParallel(Object collection, Closure cl) {
        return minParallel(createCollection(collection), cl)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its min() method using the default comparator.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the minimum of the elements in the collection.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     */
    public static <T> T minParallel(Collection<T> collection) {
        createPA(collection, retrievePool()).min()
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its min() method using the default comparator.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the minimum of the elements in the collection.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     */
    public static Object minParallel(Object collection) {
        return minParallel(createCollection(collection))
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its max() method using the supplied
     * closure as the comparator.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the maximum of the elements in the collection.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * If the supplied closure takes two arguments it is used directly as a comparator.
     * If the supplied closure takes one argument, the values returned by the supplied closure for individual elements are used for comparison by the implicit comparator.
     * @param cl A one or two-argument closure
     */
    public static <T> T maxParallel(Collection<T> collection, Closure cl) {
        createPA(collection, retrievePool()).max(createComparator(cl) as Comparator)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its max() method using the supplied
     * closure as the comparator.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the maximum of the elements in the collection.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * If the supplied closure takes two arguments it is used directly as a comparator.
     * If the supplied closure takes one argument, the values returned by the supplied closure for individual elements are used for comparison by the implicit comparator.
     * @param cl A one or two-argument closure
     */
    public static Object maxParallel(Object collection, Closure cl) {
        return maxParallel(createCollection(collection), cl)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its max() method using the default comparator.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the maximum of the elements in the collection.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     */
    public static <T> T maxParallel(Collection<T> collection) {
        createPA(collection, retrievePool()).max()
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its max() method using the default comparator.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the maximum of the elements in the collection.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     */
    public static Object maxParallel(Object collection) {
        return maxParallel(createCollection(collection))
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and summarizes its elements using the foldParallel()
     * method with the + operator and the reduction operation.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the sum of the elements in the collection.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     */
    public static <T> T sumParallel(Collection<T> collection) {
        foldParallel(collection) {a, b -> a + b}
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and summarizes its elements using the foldParallel()
     * method with the + operator and the reduction operation.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the sum of the elements in the collection.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     */
    public static Object sumParallel(Object collection) {
        return sumParallel(createCollection(collection))
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its reduce() method using the supplied
     * closure as the reduction operation.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the reduction result of the elements in the collection.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     */
    public static <T> T foldParallel(Collection<T> collection, Closure cl) {
        createPA(collection, retrievePool()).reduce(cl as Reducer, null)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its reduce() method using the supplied
     * closure as the reduction operation.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the reduction result of the elements in the collection.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     */
    public static Object foldParallel(Object collection, Closure cl) {
        return foldParallel(createCollection(collection), cl)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its reduce() method using the supplied
     * closure as the reduction operation.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the reduction result of the elements in the collection.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * @param seed A seed value to initialize the operation
     */
    public static <T> T foldParallel(Collection<T> collection, seed, Closure cl) {
        createPA(collection.plus(seed), retrievePool()).reduce(cl as Reducer, null)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its reduce() method using the supplied
     * closure as the reduction operation.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the reduction result of the elements in the collection.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * @param seed A seed value to initialize the operation
     */
    public static Object foldParallel(Object collection, seed, Closure cl) {
        return foldParallel(createCollection(collection), seed, cl)
    }

    /**
     * Creates a PAWrapper around a ParallelArray wrapping te elements of the original collection.
     * This allows further parallel processing operations on the collection to chain and so effectively leverage the underlying
     * ParallelArray implementation.
     */
    public static <T> PAWrapper<T> getParallel(Collection<T> collection) {
        new PAWrapper(createPA(collection, retrievePool()))
    }

    /**
     * Creates a PAWrapper around a ParallelArray wrapping te elements of the original collection.
     * This allows further parallel processing operations on the collection to chain and so effectively leverage the underlying
     * ParallelArray implementation.
     */
    public static PAWrapper getParallel(Object collection) {
        return getParallel(createCollection(collection))
    }

    /**
     * Builds a comparator depending on the number of arguments accepted by the supplied closure.
     */
    @SuppressWarnings("GroovyMultipleReturnPointsPerMethod")
    static Closure createComparator(final Closure handler) {
        if (handler.maximumNumberOfParameters == 2) return handler
        else return {a, b -> handler(a).compareTo(handler(b))}
    }
}

/**
 * Wraps a ParallelArray instance in map/reduce operation chains.
 */
abstract class AbstractPAWrapper<T> {

    /**
     * The wrapper ParallelArray instance
     */
    final def pa

    /**
     * Creates an instance wrapping the supplied instance of ParallelArray
     */
    def AbstractPAWrapper(final pa) {
        this.pa = pa
    }

    /**
     * Reconstructs a collection from the wrapped ParallelArray instance
     * @return A collection containing all elements of the wrapped ParallelArray
     */
    public final Object getCollection() {
        this.pa.all().asList() as ArrayList
    }

    /**
     * Performs a parallel reduce operation. It will use the supplied two-argument closure to gradually reduce two elements into one.
     * @param cl A two-argument closure merging two elements into one. The return value of the closure will replace the original two elements.
     * @return The product of reduction
     */
    public final T reduce(Closure cl) {
        pa.all().reduce(cl as Reducer, null)
    }

    /**
     * Performs a parallel reduce operation. It will use the supplied two-argument closure to gradually reduce two elements into one.
     * @param cl A two-argument closure merging two elements into one. The return value of the closure will replace the original two elements.
     * @return The product of reduction
     */
    public final T reduce(seed, Closure cl) {
        final def newPA = pa.all()
        newPA.appendElement(seed)
        newPA.reduce(cl as Reducer, null)
    }

    /**
     * Summarizes all elements of the collection in parallel using the "plus()" operator of the elements
     * @return The summary od all elements in the collection
     */
    public final T sum() {
        reduce {a, b -> a + b}
    }

    /**
     * Size of the collection
     * @return The number of elements in the collection
     */
    public final int size() {
        pa.size()
    }

    /**
     * Finds in parallel the minimum of all values in the collection. The implicit comparator is used.
     * @return The minimum element of the collection
     */
    public final T min() {
        pa.min()
    }

    /**
     * Finds in parallel the minimum of all values in the collection. The supplied comparator is used.
     * If the supplied closure takes two arguments it is used directly as a comparator.
     * If the supplied closure takes one argument, the values returned by the supplied closure for individual elements are used for comparison by the implicit comparator.
     * @param cl A one or two-argument closure
     * @return The minimum element of the collection
     */
    public final T min(Closure cl) {
        return pa.min(GParsPoolUtil.createComparator(cl) as Comparator)
    }

    /**
     * Finds in parallel the maximum of all values in the collection. The implicit comparator is used.
     * @return The maximum element of the collection
     */
    public final T max() {
        pa.max()
    }

    /**
     * Finds in parallel the maximum of all values in the collection. The supplied comparator is used.
     * If the supplied closure takes two arguments it is used directly as a comparator.
     * If the supplied closure takes one argument, the values returned by the supplied closure for individual elements are used for comparison by the implicit comparator.
     * @param cl A one or two-argument closure
     * @return The maximum element of the collection
     */
    public final T max(Closure cl) {
        pa.max(GParsPoolUtil.createComparator(cl) as Comparator)
    }

    /**
     * Applies concurrently the supplied function to all elements in the collection, returning a collection containing
     * the transformed values.
     * @param A closure calculating a transformed value from the original one
     * @return A collection holding the new values
     */
    public final AbstractPAWrapper map(Closure cl) {
        new MappedPAWrapper(pa.withMapping({cl(it)} as Mapper))
    }

    /**
     * Filters concurrently elements in the collection based on the outcome of the supplied function on each of the elements.
     * @param A closure indicating whether to propagate the given element into the filtered collection
     * @return A collection holding the allowed values
     */
    public AbstractPAWrapper filter(Closure cl) {
        new PAWrapper(pa.withFilter({cl(it)} as Predicate))
    }
}

/**
 * The default ParallelArray wrapper class
 */
final class PAWrapper<T> extends AbstractPAWrapper {
    def PAWrapper(final pa) { super(pa) }
}

/**
 * The ParallelArray wrapper used after the map() operation
 */
final class MappedPAWrapper<T> extends AbstractPAWrapper {

    def MappedPAWrapper(final ParallelArray.WithMapping pa) {
        super(pa)
    }

    /**
     * Filters concurrently elements in the collection based on the outcome of the supplied function on each of the elements.
     * @param A closure indicating whether to propagate the given element into the filtered collection
     * @return A collection holding the allowed values
     */
    public final AbstractPAWrapper filter(Closure cl) {
        new PAWrapper(pa.all().withFilter({cl(it)} as Predicate))
    }
}
