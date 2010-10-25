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
import groovyx.gpars.util.PAUtils
import java.lang.ref.ReferenceQueue
import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import jsr166y.forkjoin.ForkJoinExecutor
import jsr166y.forkjoin.ForkJoinPool
import jsr166y.forkjoin.Ops.Mapper
import jsr166y.forkjoin.Ops.Predicate
import jsr166y.forkjoin.Ops.Procedure
import jsr166y.forkjoin.Ops.Reducer
import jsr166y.forkjoin.ParallelArray
import jsr166y.forkjoin.RecursiveTask
import static groovyx.gpars.util.ParallelCollectionsUtil.buildClosureForMaps
import static groovyx.gpars.util.ParallelCollectionsUtil.buildClosureForMapsWithIndex
import static groovyx.gpars.util.ParallelCollectionsUtil.buildResultMap
import static groovyx.gpars.util.ParallelCollectionsUtil.createCollection

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

    final static def MEMOIZE_NULL = new NullValue()

    /**
     * Allows timeouts for async operations
     */
    private static final Timer timer = new Timer('GParsTimeoutTimer', true)

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
     * This variant will keep all values forever, i.e. till the closure gets garbage-collected.
     * The returned function can be safely used concurrently from multiple threads, however, the implementation
     * values high average-scenario performance and so concurrent calls on the memoized function with identical argument values
     * may not necessarily be able to benefit from each other's cached return value. With this having been mentioned,
     * the performance trade-off still makes concurrent use of memoized functions safe and highly recommended.
     *
     * The cache gets garbage-collected together with the memoized closure.
     * @return A new function forwarding to the original one while caching the results
     */
    public static Closure memoize(final Closure cl) {
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
     *
     * The cache gets garbage-collected together with the memoized closure.
     * @param maxCacheSize The maximum size the cache can grow to
     * @return A new function forwarding to the original one while caching the results
     */
    public static Closure memoizeAtMost(final Closure cl, int maxCacheSize) {
        if (maxCacheSize < 0) throw new IllegalArgumentException("A non-negative number is required as the maxCacheSize parameter for memoizeAtMost.")

        return buildMemoizeFunction(new LRUProtectionStorage(maxCacheSize).asSynchronized(), cl)
    }

    private static def buildMemoizeFunction(cache, Closure cl) {
        return {Object... args ->
            final def key = args?.toList() ?: []
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
     *
     * The cache gets garbage-collected together with the memoized closure.
     */
    public static Closure memoizeAtLeast(final Closure cl, int protectedCacheSize) {
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
     *
     * The cache gets garbage-collected together with the memoized closure.
     */
    public static Closure memoizeBetween(final Closure cl, int protectedCacheSize, int maxCacheSize) {
        if (protectedCacheSize < 0) throw new IllegalArgumentException("A non-negative number is required as the protectedCacheSize parameter for memoizeBetween.")
        if (maxCacheSize < 0) throw new IllegalArgumentException("A non-negative number is required as the maxCacheSize parameter for memoizeBetween.")
        if (protectedCacheSize > maxCacheSize) throw new IllegalArgumentException("The maxCacheSize parameter to memoizeBetween is required to be greater or equal to the protectedCacheSize parameter.")

        return buildSoftReferenceMemoizeFunction(protectedCacheSize, new LRUProtectionStorage(maxCacheSize).asSynchronized(), cl)
    }

    private static def buildSoftReferenceMemoizeFunction(int protectedCacheSize, cache, Closure cl) {
        def lruProtectionStorage = protectedCacheSize > 0 ?
            new LRUProtectionStorage(protectedCacheSize) :
            new NullProtectionStorage() //Nothing should be done when no elements need protection against eviction

        final ReferenceQueue queue = new ReferenceQueue()

        return {Object... args ->
            if (queue.poll() != null) cleanUpNullReferences(cache, queue)  //if something has been evicted, do a clean-up
            final def key = args?.toList() ?: []
            def result = cache[key]?.get()
            if (result == null) {
                result = cl.call(* args)
                if (result == null) {
                    result = MEMOIZE_NULL
                }
                cache[key] = new SoftReference(result, queue)
            }
            lruProtectionStorage.touch(key, result)
            result == MEMOIZE_NULL ? null : result
        }
    }

    private static void cleanUpNullReferences(final cache, final queue) {
        //noinspection GroovyEmptyStatementBody
        while (queue.poll() != null) {
        }  //empty the reference queue
        cache.findAllParallel({entry -> entry.value.get() == null}).eachParallel {entry -> cache.remove entry.key}
    }

    @SuppressWarnings("GroovyMultipleReturnPointsPerMethod")
    private static <T> ParallelArray<T> createPA(final Object collection, final ForkJoinExecutor pool) {
        if (collection instanceof Object[]) {
            return createPAFromArray(collection, pool)
        }
        if (collection instanceof Map) {
            return createPAFromArray(PAUtils.createArray((Map) collection), pool)
        }
        if (collection.respondsTo('toArray')) {
            return createPAFromCollection(collection, pool)
        }
        if (collection instanceof CharSequence) {
            return createPAFromArray(PAUtils.createArray((CharSequence) collection), pool)
        }
        if (collection instanceof Iterable) {
            return createPAFromCollection(PAUtils.createCollection((Iterable) collection), pool)
        }
        if (collection instanceof Iterator) {
            return createPAFromCollection(PAUtils.createCollection((Iterator) collection), pool)
        }
        return createPAFromCollection(createCollection(collection), pool)
        //todo generics should not cause warnings
        //todo finish all methods
    }

    private static <T> ParallelArray<T> createPAFromCollection(final def collection, final ForkJoinExecutor pool) {
        return ParallelArray.createFromCopy(collection.toArray(new T[collection.size()]), pool)
    }

    private static <T> ParallelArray<T> createPAFromArray(final T[] array, final ForkJoinExecutor pool) {
        return ParallelArray.createFromCopy(array, pool)
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
        //noinspection GroovyGetterCallCanBePropertyAccess
        if (!collection.isTransparent()) collection.getMetaClass().mixin(TransparentParallel)
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
        eachParallelPA(createPAFromCollection(collection, retrievePool()), cl)
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
        eachParallelPA(createPA(collection, retrievePool()), cl)
        return collection
    }

    /**
     * Creates a Parallel Array out of the supplied map and invokes the withMapping() method using the supplied
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
    public static Object eachParallel(Map collection, Closure cl) {
        eachParallelPA(createPA(collection, retrievePool()), buildClosureForMaps(cl))
        return collection
    }

    private static <T> void eachParallelPA(ParallelArray<T> pa, Closure cl) {
        pa.apply({cl(it)} as Procedure)
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
        createPAFromCollection(indexedCollection, retrievePool()).withMapping({cl(it[0], it[1])} as Mapper).all()
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
     * Does parallel eachWithIndex on maps
     */
    public static Object eachWithIndexParallel(Map collection, Closure cl) {
        eachWithIndexParallel(createCollection(collection), buildClosureForMapsWithIndex(cl))
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
        createPAFromCollection(collection, retrievePool()).withMapping({cl(it)} as Mapper).all().asList()
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
        createPA(collection, retrievePool()).withMapping({cl(it)} as Mapper).all().asList()
    }

    /**
     * Creates a Parallel Array out of the supplied map and invokes the withMapping() method using the supplied
     * closure as the transformation operation.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a collection of values from the resulting Parallel Array.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>collectParallel(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].collectParallel {Number number -> number * 10}*     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     *}*/
    public static Collection<Object> collectParallel(Map collection, Closure cl) {
        createPA(collection, retrievePool()).withMapping(buildClosureForMaps(cl) as Mapper).all().asList()
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
        (Collection<T>) findAllParallelPA(createPAFromCollection(collection, retrievePool()), cl)
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
        (Collection<Object>) findAllParallelPA(createPA(collection, retrievePool()), cl)
    }

    /**
     * Creates a Parallel Array out of the supplied map and invokes the withFilter() method using the supplied
     * closure as the filter predicate.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a collection of values from the resulting Parallel Array.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>findAllParallel(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].findAllParallel {Number number -> number > 3}*     assertEquals(new HashSet([4, 5]), result)
     *}*/
    public static <K, V> Map<K, V> findAllParallel(Map<K, V> collection, Closure cl) {
        buildResultMap((List<Map.Entry<K, V>>) findAllParallelPA(createPA(collection, retrievePool()), buildClosureForMaps(cl)))
    }

    private static <T> Collection<T> findAllParallelPA(ParallelArray<T> pa, Closure cl) {
        (Collection<T>) pa.withFilter({cl(it) as Boolean} as Predicate).all().asList()
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
    public static <T> T findParallel(Collection<T> collection, Closure cl) {
        findParallelPA(createPAFromCollection(collection, retrievePool()), cl)
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
        findParallelPA(createPA(collection, retrievePool()), cl)
    }

    /**
     * Creates a Parallel Array out of the supplied map and invokes the withFilter() method using the supplied
     * closure as the filter predicate.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a value from the resulting Parallel Array with the minimum index.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>findParallel(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].findParallel {Number number -> number > 3}*     assert (result in [4, 5])
     *}*/
    public static <K, V> Map.Entry<K, V> findParallel(Map<K, V> collection, Closure cl) {
        (Map.Entry<K, V>) findParallelPA(createPA(collection, retrievePool()), buildClosureForMaps(cl))
    }

    private static <T> T findParallelPA(ParallelArray<T> pa, Closure cl) {
        final ParallelArray found = pa.withFilter({cl(it) as Boolean} as Predicate).all()
        if (found.size() > 0) found.get(0)
        else return null
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
        findAnyParallelPA(createPAFromCollection(collection, retrievePool()), cl)
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
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].findAnyParallel {Number number -> number > 3}*     assert (result in [4, 5])
     *}*/
    public static Object findAnyParallel(Object collection, Closure cl) {
        findAnyParallelPA(createPA(collection, retrievePool()), cl)
    }

    /**
     * Creates a Parallel Array out of the supplied map and invokes the withFilter() method using the supplied
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
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].findAnyParallel {Number number -> number > 3}*     assert (result in [4, 5])
     *}*/
    public static Object findAnyParallel(Map collection, Closure cl) {
        findAnyParallelPA(createPA(collection, retrievePool()), buildClosureForMaps(cl))
    }

    private static <T> T findAnyParallelPA(ParallelArray<T> pa, Closure cl) {
        pa.withFilter({cl(it) as Boolean} as Predicate).any()
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
        (Collection<T>) grepParallelPA(createPAFromCollection(collection, retrievePool()), filter)
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
        (Collection<Object>) grepParallelPA(createPA(collection, retrievePool()), filter)
    }

    /**
     * Creates a Parallel Array out of the supplied map and invokes the withFilter() method using the supplied
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
    public static <K, V> Map<K, V> grepParallel(Map<K, V> collection, filter) {
        (Map<K, V>) buildResultMap((List<Map.Entry<K, V>>) grepParallelPA(createPA(collection, retrievePool()), filter in Closure ? buildClosureForMaps(filter) : filter))
    }

    private static <T> Collection<T> grepParallelPA(ParallelArray<T> pa, filter) {
        pa.withFilter({filter.isCase it} as Predicate).all().asList()
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withFilter() method using the supplied
     * rule as the filter predicate.
     * The filter will be effectively used concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a collection of values from the resulting Parallel Array.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>grepParallel(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     */
    public static <T> Collection<T> splitParallel(Collection<T> collection, filter) {
        final def groups = groupByParallel(collection, filter)
        (Collection<T>) [groups[true] ?: [], groups[false] ?: []]
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
        final def groups = groupByParallelPA(createPA(collection, retrievePool()), filter)
        (Collection<Object>) [groups[true] ?: [], groups[false] ?: []]
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
        createPAFromCollection(collection, retrievePool()).withFilter({filter == it} as Predicate).size()
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
        createPA(collection, retrievePool()).withFilter({filter == it} as Predicate).size()
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
        createPAFromCollection(collection, retrievePool()).withFilter({cl(it) as Boolean} as Predicate).any() != null
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
        createPA(collection, retrievePool()).withFilter({cl(it) as Boolean} as Predicate).any() != null
    }

    /**
     * Creates a Parallel Array out of the supplied map and invokes the withFilter() method using the supplied
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
    public static boolean anyParallel(Map collection, Closure cl) {
        final def mapClosure = buildClosureForMaps(cl)
        createPA(collection, retrievePool()).withFilter({mapClosure(it) as Boolean} as Predicate).any() != null
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
        createPAFromCollection(collection, retrievePool()).withFilter({!(cl(it) as Boolean)} as Predicate).any() == null
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
        createPA(collection, retrievePool()).withFilter({!(cl(it) as Boolean)} as Predicate).any() == null
    }

    /**
     * Creates a Parallel Array out of the supplied map and invokes the withFilter() method using the supplied
     * closure as the filter predicate.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a boolean value indicating, whether all the elements
     * of the collection meet the predicate.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>everyParallel(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * GParsPool.withPool(5) {*     assert ![1, 2, 3, 4, 5].everyParallel {Number number -> number > 3}*     assert [1, 2, 3].everyParallel() {Number number -> number <= 3}*}*/
    public static boolean everyParallel(Map collection, Closure cl) {
        def mapClosure = buildClosureForMaps(cl)
        createPA(collection, retrievePool()).withFilter({!(mapClosure(it) as Boolean)} as Predicate).any() == null
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withMapping() method using the supplied
     * closure as the mapping predicate.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a map of groups of the original elements.
     * Elements in the same group gave identical results when the supplied closure was invoked on them.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>groupByParallel(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * GParsPool.withPool {*     assert ([1, 2, 3, 4, 5].groupByParallel {Number number -> number % 2}).size() == 2
     *}*/
    public static <T> Map groupByParallel(Collection<T> collection, Closure cl) {
        return groupByParallelPA(createPAFromCollection(collection, retrievePool()), cl)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withMapping() method using the supplied
     * closure as the mapping predicate.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a map of groups of the original elements.
     * Elements in the same group gave identical results when the supplied closure was invoked on them.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>groupByParallel(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * GParsPool.withPool {*     assert ([1, 2, 3, 4, 5].groupByParallel {Number number -> number % 2}).size() == 2
     *}*/
    public static Map groupByParallel(Object collection, Closure cl) {
        groupByParallelPA(createPA(collection, retrievePool()), cl)
    }

    private static <T> Map groupByParallelPA(ParallelArray<T> pa, Closure cl) {
        final def map = new ConcurrentHashMap()
        eachParallelPA(pa, {
            def result = cl(it)
            final def myList = [it].asSynchronized()
            def list = map.putIfAbsent(result, myList)
            if (list != null) list.add(it)
        })
        return map

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
        createPAFromCollection(collection, retrievePool()).min(createComparator(cl) as Comparator)
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
        createPA(collection, retrievePool()).min(createComparator(cl) as Comparator)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its min() method using the default comparator.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the minimum of the elements in the collection.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     */
    public static <T> T minParallel(Collection<T> collection) {
        createPAFromCollection(collection, retrievePool()).min()
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its min() method using the default comparator.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the minimum of the elements in the collection.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     */
    public static Object minParallel(Object collection) {
        createPA(collection, retrievePool()).min()
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
        createPAFromCollection(collection, retrievePool()).max(createComparator(cl) as Comparator)
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
        createPA(collection, retrievePool()).max(createComparator(cl) as Comparator)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its max() method using the default comparator.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the maximum of the elements in the collection.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     */
    public static <T> T maxParallel(Collection<T> collection) {
        createPAFromCollection(collection, retrievePool()).max()
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its max() method using the default comparator.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the maximum of the elements in the collection.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     */
    public static Object maxParallel(Object collection) {
        createPA(collection, retrievePool()).max()
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
        foldParallel(collection) {a, b -> a + b}
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
        createPAFromCollection(collection, retrievePool()).reduce(cl as Reducer, null)
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
     * @param seed A seed value to initialize the operation
     */
    public static <T> T foldParallel(Collection<T> collection, seed, Closure cl) {
        createPAFromCollection(collection.plus(seed), retrievePool()).reduce(cl as Reducer, null)
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
        final ParallelArray pa = createPA(collection, retrievePool())
        pa.appendElement(seed)
        pa.reduce(cl as Reducer, null)
    }

    /**
     * Creates a PAWrapper around a ParallelArray wrapping te elements of the original collection.
     * This allows further parallel processing operations on the collection to chain and so effectively leverage the underlying
     * ParallelArray implementation.
     */
    public static <T> PAWrapper<T> getParallel(Collection<T> collection) {
        new PAWrapper(createPAFromCollection(collection, retrievePool()))
    }

    /**
     * Creates a PAWrapper around a ParallelArray wrapping te elements of the original collection.
     * This allows further parallel processing operations on the collection to chain and so effectively leverage the underlying
     * ParallelArray implementation.
     */
    public static PAWrapper getParallel(Object collection) {
        new PAWrapper(createPA(collection, retrievePool()))
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
     * Returns a sorted parallel collection
     * If the supplied closure takes two arguments it is used directly as a comparator.
     * If the supplied closure takes one argument, the values returned by the supplied closure for individual elements are used for comparison by the implicit comparator.
     * @param cl A one or two-argument closure
     * @return A sorted collection holding all the elements
     */
    public final AbstractPAWrapper sort(Closure cl = {it}) {
        def npa = pa.all()
        npa.sort(GParsPoolUtil.createComparator(cl) as Comparator)
        return new PAWrapper(npa)
    }

    /**
     * Performs parallel groupBy operation.
     * After all the elements have been processed, the method returns a map of groups of the original elements.
     * Elements in the same group gave identical results when the supplied closure was invoked on them.
     * Please note that the method returns a regular map, not a PAWrapper instance.
     * You can use the "getParallel()" method on the returned map to turn it into a parallel collection again.
     * @param cl A single-argument closure returning the value to use for grouping (the key in the resulting map).
     * @return A map following the Groovy specification for groupBy
     */
    public Map groupBy(Closure cl) {
        return combineImpl(cl, {it}, {[]}, {list, item -> list << item})
    }

    /**
     * Performs a parallel combine operation.
     * The operation accepts a collection of tuples (two-element lists). The element at position 0 is treated as a key,
     * while the element at position 1 is considered to be the value.
     * By running 'combine' on such a collection of tuples, you'll back a map of get items with the same keys (the key is represented by tuple[0])
     * to be combined into a single element under their common key.
     * E.g. [[a, b], [c, d], [a, e], [c, f]] will be combined into [a : b+e, c : d+f], while the '+' operation on the values needs to be provided by the user as the accumulation closure.
     *
     * The 'accumulation function' argument needs to specify a function to use for combining (accumulating) the values belonging to the same key.
     * An 'initial accumulator value' needs to be provided as well. Since the 'combine' method processes items in parallel, the 'initial accumulator value' will be reused multiple times.
     * Thus the provided value must allow for reuse. It should be either a cloneable or immutable value or a closure returning a fresh initial accumulator each time requested.
     * Good combinations of accumulator functions and reusable initial values include:
     * <br/>accumulator = {List acc, value -> acc << value} initialValue = []
     * <br/>accumulator = {List acc, value -> acc << value} initialValue = {-> []}* <br/>accumulator = {int sum, int value -> acc + value} initialValue = 0
     * <br/>accumulator = {int sum, int value -> sum + value} initialValue = {-> 0}* <br/>accumulator = {ShoppingCart cart, Item value -> cart.addItem(value)} initialValue = {-> new ShoppingCart()}* <br/>
     * The return type is a map.
     * E.g. [['he', 1], ['she', 2], ['he', 2], ['me', 1], ['she, 5], ['he', 1] with the initial value provided a 0 will be combined into
     * ['he' : 4, 'she' : 7, 'he', : 2, 'me' : 1]
     *
     * Please note that the method returns a regular map, not a PAWrapper instance.
     * You can use the "getParallel()" method on the returned map to turn it into a parallel collection again.
     * @param initialValue The initial value for an accumulator. Since it will be used repeatedly, it should be either an unmodifiable value, a cloneable instance or a closure returning a fresh initial/empty accumulator each time requested
     * @param accumulator A two-argument closure, first argument being the accumulator and second holding the currently processed value. The closure is supposed to returned a modified accumulator after accumulating the value.
     * @return A map holding the final accumulated values for each unique key in the original collection of tuples.
     */
    public Map combine(Object initialValue, Closure accumulation) {
        switch (initialValue) {
            case Closure: return combineImpl((Closure) initialValue, accumulation)
            case Cloneable: return combineImpl({initialValue.clone()}, accumulation)
            default: return combineImpl({initialValue}, accumulation)
        }
    }

    public Map combineImpl(Closure initialValue, Closure accumulation) {
        combineImpl({it[0]}, {it[1]}, initialValue, accumulation)
    }

    public Map combineImpl(extractKey, extractValue, Closure initialValue, Closure accumulation) {

        def result = reduce {a, b ->
            if (a in CombineHolder) {
                if (b in CombineHolder) return a.merge(b, accumulation, initialValue)
                else return a.addToMap(extractKey(b), extractValue(b), accumulation, initialValue)
            } else {
                def aKey = extractKey(a)
                final Object aValue = extractValue(a)
                if (b in CombineHolder) return b.addToMap(aKey, aValue, accumulation, initialValue)
                else {
                    def bKey = extractKey(b)
                    final Object bValue = extractValue(b)

                    if (aKey == bKey) {
                        def c = accumulation(accumulation(initialValue(), aValue), bValue)
                        return [(aKey): c] as CombineHolder
                    }
                    else {
                        def c = accumulation(initialValue(), aValue)
                        def holder = [(aKey): c] as CombineHolder
                        return holder.addToMap(bKey, bValue, accumulation, initialValue)
                    }
                }
            }
        }
        return result.getContent()
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
 * Holds a temporary reduce result for groupBy
 */
//private class GroupByHolder extends CombineHolder {
//
//    def GroupByHolder(final content) {
//        super([], {List a, List b -> a.addAll(b)}, content)
//    }
//}

private class CombineHolder {

    @Delegate final Map content

    def CombineHolder(final content) {
        this.content = content;
    }

    final CombineHolder merge(CombineHolder other, final Closure accumulation, final Closure initialValue) {
        for (item in other.entrySet()) {
            for (value in item.value) {
                addToMap(item.key, value, accumulation, initialValue)
            }
        }
        return this
    }

    def CombineHolder addToMap(final Object key, final Object item, final Closure accumulation, final Closure initialValue) {
        def currentValue = content[key] ?: initialValue()
        content[key] = accumulation(currentValue, item)
        return this
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
