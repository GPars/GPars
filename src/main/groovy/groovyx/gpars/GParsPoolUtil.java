// GPars - Groovy Parallel Systems
//
// Copyright © 2008--2011  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars;

import groovy.lang.Closure;
import groovy.time.Duration;
import groovyx.gpars.extra166y.Ops;
import groovyx.gpars.extra166y.ParallelArray;
import groovyx.gpars.memoize.LRUProtectionStorage;
import groovyx.gpars.pa.CallAsyncTask;
import groovyx.gpars.pa.CallClosure;
import groovyx.gpars.pa.ClosureMapper;
import groovyx.gpars.pa.ClosureNegationPredicate;
import groovyx.gpars.pa.ClosurePredicate;
import groovyx.gpars.pa.ClosureReducer;
import groovyx.gpars.pa.GParsPoolUtilHelper;
import groovyx.gpars.pa.PAWrapper;
import groovyx.gpars.pa.SumClosure;
import groovyx.gpars.scheduler.FJPool;
import groovyx.gpars.util.GeneralTimer;
import groovyx.gpars.util.PAUtils;
import jsr166y.ForkJoinPool;
import jsr166y.RecursiveTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import static groovyx.gpars.util.PAGroovyUtils.createCollection;
import static groovyx.gpars.util.PAUtils.buildClosureForMaps;
import static groovyx.gpars.util.PAUtils.buildClosureForMapsWithIndex;
import static groovyx.gpars.util.PAUtils.buildResultMap;
import static groovyx.gpars.util.PAUtils.createComparator;
import static groovyx.gpars.util.PAUtils.createGroupByClosure;
import static java.util.Arrays.asList;

/**
 * This class forms the core of the DSL initialized by <i>GParsPool</i>. The static methods of <i>GParsPoolUtil</i>
 * get attached to their first arguments (the Groovy Category mechanism) and can be then invoked as if they were part of
 * the argument classes.
 *
 * @author Vaclav Pech
 * @author Robert Fischer
 *         Date: Mar 10, 2010
 * @see groovyx.gpars.GParsPool
 */
public class GParsPoolUtil {

    /**
     * Allows timeouts for async operations
     */
    private static final GeneralTimer timer = GParsConfig.retrieveDefaultTimer("GParsTimeoutTimer", true);

    private static ForkJoinPool retrievePool() {
        final ForkJoinPool pool = (ForkJoinPool) GParsPool.retrieveCurrentPool();
        if (pool == null) throw new IllegalStateException("No ForkJoinPool available for the current thread");
        return pool;
    }

    /**
     * schedules the supplied closure for processing in the underlying thread pool.
     */
    public static <T> Future<T> callParallel(final Closure<T> task) {
        final ForkJoinPool pool = (ForkJoinPool) GParsPool.retrieveCurrentPool();
        if (pool == null) throw new IllegalStateException("No ForkJoinPool available for the current thread.");
        return pool.submit(new CallAsyncTask<T>(task));
    }

    /**
     * Calls a closure in a separate thread supplying the given arguments, returning a future for the potential return value.
     */
    public static <T> Future<T> callAsync(final Closure<T> cl, final Object... args) {
        return GParsPoolUtilHelper.callAsync(cl, args);
    }

    /**
     * Calls a closure in a separate thread supplying the given arguments, returning a future for the potential return value.
     * Also allows the asynchronous calculation to be cancelled after a given timeout.
     * In order to allow cancellation, the asynchronously running code must keep checking the _interrupted_ flag of its
     * own thread and cease the calculation once the flag is set to true.
     *
     * @param timeout The timeout in milliseconds to wait before the calculation gets cancelled.
     */
    public static <T> Future<T> callTimeoutAsync(final Closure<T> cl, final long timeout, final Object... args) {
        final Future<T> f = callAsync(cl, args);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                f.cancel(true);
            }
        }, timeout);
        return f;
    }

    /**
     * Calls a closure in a separate thread supplying the given arguments, returning a future for the potential return value.
     * Also allows the asynchronous calculation to be cancelled after a given timeout.
     * In order to allow cancellation, the asynchronously running code must keep checking the _interrupted_ flag of its
     * own thread and cease the calculation once the flag is set to true.
     *
     * @param timeout The timeout to wait before the calculation gets cancelled.
     */
    public static <T> Future<T> callTimeoutAsync(final Closure<T> cl, final Duration timeout, final Object... args) {
        return callTimeoutAsync(cl, timeout.toMilliseconds(), args);
    }

    /**
     * Submits the task for asynchronous processing returning the Future received from the executor service.
     * Allows for the following syntax:
     * <pre>
     * executorService << {println 'Inside parallel task'}* </pre>
     */
    public static <T> Future<T> leftShift(final ForkJoinPool pool, final Closure<T> task) {
        return pool.submit(new RecursiveTask<T>() {
            @Override
            protected T compute() {
                return task.call();
            }
        });
    }

    /**
     * Creates an asynchronous variant of the supplied closure, which, when invoked returns a future for the potential return value
     */
    public static Closure async(final Closure cl) {
        return GParsPoolUtilHelper.async(cl);
    }

    /**
     * Creates an asynchronous and composable variant of the supplied closure, which, when invoked returns a DataflowVariable for the potential return value
     */
    public static Closure asyncFun(final Closure original) {
        return asyncFun(original, false);
    }

    /**
     * Creates an asynchronous and composable variant of the supplied closure, which, when invoked returns a DataflowVariable for the potential return value
     */
    public static Closure asyncFun(final Closure original, final boolean blocking) {
        return GParsPoolUtilHelper.asyncFun(original, blocking);
    }

    /**
     * Creates an asynchronous and composable variant of the supplied closure, which, when invoked returns a DataflowVariable for the potential return value
     */
    public static Closure asyncFun(final Closure original, final FJPool pool) {
        return asyncFun(original, pool, false);
    }

    /**
     * Creates an asynchronous and composable variant of the supplied closure, which, when invoked returns a DataflowVariable for the potential return value
     */
    public static Closure asyncFun(final Closure original, final FJPool pool, final boolean blocking) {
        return GParsPoolUtilHelper.asyncFun(original, blocking, pool);
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
     * <p/>
     * The cache gets garbage-collected together with the memoized closure.
     *
     * @return A new function forwarding to the original one while caching the results
     */
    public static <T> Closure<T> gmemoize(final Closure<T> cl) {
        return GParsPoolUtilHelper.buildMemoizeFunction(new ConcurrentHashMap(), cl);
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
     * <p/>
     * The cache gets garbage-collected together with the memoized closure.
     *
     * @param maxCacheSize The maximum size the cache can grow to
     * @return A new function forwarding to the original one while caching the results
     */
    public static <T> Closure<T> gmemoizeAtMost(final Closure<T> cl, final int maxCacheSize) {
        if (maxCacheSize < 0)
            throw new IllegalArgumentException("A non-negative number is required as the maxCacheSize parameter for gmemoizeAtMost.");

        return GParsPoolUtilHelper.buildMemoizeFunction(Collections.synchronizedMap(new LRUProtectionStorage(maxCacheSize)), cl);
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
     * <p/>
     * The cache gets garbage-collected together with the memoized closure.
     */
    public static <T> Closure<T> gmemoizeAtLeast(final Closure<T> cl, final int protectedCacheSize) {
        if (protectedCacheSize < 0)
            throw new IllegalArgumentException("A non-negative number is required as the protectedCacheSize parameter for gmemoizeAtLeast.");

        return GParsPoolUtilHelper.buildSoftReferenceMemoizeFunction(protectedCacheSize, new ConcurrentHashMap(), cl);
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
     * <p/>
     * The cache gets garbage-collected together with the memoized closure.
     */
    public static <T> Closure<T> gmemoizeBetween(final Closure<T> cl, final int protectedCacheSize, final int maxCacheSize) {
        if (protectedCacheSize < 0)
            throw new IllegalArgumentException("A non-negative number is required as the protectedCacheSize parameter for gmemoizeBetween.");
        if (maxCacheSize < 0)
            throw new IllegalArgumentException("A non-negative number is required as the maxCacheSize parameter for gmemoizeBetween.");
        if (protectedCacheSize > maxCacheSize)
            throw new IllegalArgumentException("The maxCacheSize parameter to gmemoizeBetween is required to be greater or equal to the protectedCacheSize parameter.");

        return GParsPoolUtilHelper.buildSoftReferenceMemoizeFunction(protectedCacheSize, Collections.synchronizedMap(new LRUProtectionStorage(maxCacheSize)), cl);
    }

    private static <K, V> ParallelArray<Map.Entry<K, V>> createPA(final Map<K, V> collection, final ForkJoinPool pool) {
        return GParsPoolUtilHelper.createPAFromArray(PAUtils.createArray(collection), pool);
    }

    /**
     * Overrides the iterative methods like each(), collect() and such, so that they call their parallel variants from the GParsPoolUtil class
     * like eachParallel(), collectParallel() and such.
     * The first time it is invoked on a collection the method creates a TransparentParallel class instance and mixes it
     * in the object it is invoked on. After mixing-in, the isConcurrent() method will return true.
     * Delegates to GParsPoolUtil.makeConcurrent().
     *
     * @param collection The object to make transparent
     * @return The instance of the TransparentParallel class wrapping the original object and overriding the iterative methods with new parallel behavior
     */
    public static Object makeConcurrent(final Object collection) {
        return GParsPoolUtilHelper.makeConcurrent(collection);
    }

    /**
     * Gives the iterative methods like each() or find() the original sequential semantics.
     *
     * @param collection The collection to apply the change to
     * @return The collection itself
     */
    public static Object makeSequential(final Object collection) {
        return GParsPoolUtilHelper.makeSequential(collection);
    }

    /**
     * Makes the collection concurrent for the passed-in block of code.
     * The iterative methods like each or collect are given concurrent semantics inside the passed-in closure.
     * Once the closure finishes, the original sequential semantics of the methods is restored.
     * Must be invoked inside a withPool block.
     *
     * @param collection The collection to enhance
     * @param code       The closure to run with the collection enhanced.
     */
    public static void asConcurrent(final Object collection, final Closure code) {
        makeConcurrent(collection);
        try {
            code.call(collection);
        } finally {
            makeSequential(collection);
        }
    }

    /**
     * Indicates whether the iterative methods like each() or collect() work have been altered to work concurrently.
     */
    public static boolean isConcurrent(final Object collection) {
        return false;
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
     * <pre>
     * GParsPool.withPool {*     def result = new ConcurrentSkipListSet()
     *     [1, 2, 3, 4, 5].eachParallel {Number number -&gt; result.add(number * 10)}*     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     * }* </pre>
     * Note that the <i>result</i> variable is synchronized to prevent race conditions between multiple threads.
     */
    public static <T> Collection<T> eachParallel(final Collection<T> collection, final Closure cl) {
        GParsPoolUtilHelper.eachParallelPA(GParsPoolUtilHelper.createPAFromCollection(collection, retrievePool()), cl);
        return collection;
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
     * <pre>
     * GParsPool.withPool {*     def result = new ConcurrentSkipListSet()
     *     [1, 2, 3, 4, 5].eachParallel {Number number -&gt; result.add(number * 10)}*     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     * }* </pre>
     * Note that the <i>result</i> variable is synchronized to prevent race conditions between multiple threads.
     */
    public static <T> T eachParallel(final T collection, final Closure cl) {
        GParsPoolUtilHelper.eachParallelPA(GParsPoolUtilHelper.createPA(collection, retrievePool()), cl);
        return collection;
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
     * <pre>
     * GParsPool.withPool {*     def result = new ConcurrentSkipListSet()
     *     [1, 2, 3, 4, 5].eachParallel {Number number -&gt; result.add(number * 10)}*     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     * }* </pre>
     * Note that the <i>result</i> variable is synchronized to prevent race conditions between multiple threads.
     */
    public static <K, V> Map<K, V> eachParallel(final Map<K, V> collection, final Closure cl) {
        GParsPoolUtilHelper.eachParallelPA(createPA(collection, retrievePool()), buildClosureForMaps(cl));
        return collection;
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
     * <pre>
     * GParsPool.withPool {*     def result = new ConcurrentSkipListSet()
     *     [1, 2, 3, 4, 5].eachWithIndexParallel {Number number, int index -&gt; result.add(number * 10)}*     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     * }* </pre>
     * Note that the <i>result</i> variable is synchronized to prevent race conditions between multiple threads.
     */
    public static <T> Collection<T> eachWithIndexParallel(final Collection<T> collection, final Closure cl) {
        final List<List<Object>> indexedCollection = new ArrayList<List<Object>>();
        int index = 0;
        for (final T element : collection) {
            indexedCollection.add(asList(element, index));
            index++;
        }
        final ParallelArray<List<Object>> paFromCollection = GParsPoolUtilHelper.createPAFromCollection(indexedCollection, retrievePool());
        GParsPoolUtilHelper.eachWithIndex(paFromCollection, cl).all();
        return collection;
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
     * <pre>
     * GParsPool.withPool {*     def result = new ConcurrentSkipListSet()
     *     [1, 2, 3, 4, 5].eachWithIndexParallel {Number number, int index -&gt; result.add(number * 10)}*     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     * }* </pre>
     * Note that the <i>result</i> variable is synchronized to prevent race conditions between multiple threads.
     */
    public static <T> T eachWithIndexParallel(final T collection, final Closure cl) {
        eachWithIndexParallel(createCollection(collection), cl);
        return collection;
    }

    /**
     * Does parallel eachWithIndex on maps
     */
    public static <K, V> Map<K, V> eachWithIndexParallel(final Map<K, V> collection, final Closure cl) {
        eachWithIndexParallel(createCollection(collection), buildClosureForMapsWithIndex(cl));
        return collection;
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
     * <pre>
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].collectParallel {Number number -&gt; number * 10}*     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     * }* </pre>
     */
    public static <T> Collection<T> collectParallel(final Collection collection, final Closure<? extends T> cl) {
        return GParsPoolUtilHelper.createPAFromCollection(collection, retrievePool()).withMapping(new ClosureMapper(new CallClosure(cl))).all().asList();
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
     * <pre>
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].collectParallel {Number number -&gt; number * 10}*     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     * }* </pre>
     */
    public static <T> Collection<T> collectParallel(final Object collection, final Closure<? extends T> cl) {
        return GParsPoolUtilHelper.createPA(collection, retrievePool()).withMapping(new ClosureMapper(new CallClosure(cl))).all().asList();
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
     * <pre>
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].collectParallel {Number number -&gt; number * 10}*     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     * }* </pre>
     */
    public static <T> Collection<T> collectParallel(final Map collection, final Closure<? extends T> cl) {
        return createPA(collection, retrievePool()).withMapping(new ClosureMapper(buildClosureForMaps(cl))).all().asList();
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withMapping() method using the supplied
     * <code>projection</code> closure as the transformation operation. The <code>projection</code> closure should return a
     * (possibly empty) collection of items which are subsequently flattened to produce a new collection.
     * The <code>projection</code> closure will be effectively invoked concurrently on the elements of the original collection.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>collectManyParallel(Closure projection)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * <pre>
     * GParsPool.withPool {*     def squaresAndCubesOfOdds = [1, 2, 3, 4, 5].collectManyParallel { Number number -&gt;
     *         number % 2 ? [number ** 2, number ** 3] : []
     * }*     assert squaresAndCubesOfOdds == [1, 1, 9, 27, 25, 125]
     * }* </pre>
     */
    public static <T> List<T> collectManyParallel(final Collection collection, final Closure<Collection<? extends T>> projection) {
        return (List<T>) GParsPoolUtilHelper.createPAFromCollection(collection, retrievePool()).withMapping(new ClosureMapper(new CallClosure(projection))).reduce(new ClosureReducer(SumClosure.getInstance()), null);
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withMapping() method using the supplied
     * <code>projection</code> closure as the transformation operation. The <code>projection</code> closure should return a
     * (possibly empty) collection of items which are subsequently flattened to produce a new collection.
     * The <code>projection</code> closure will be effectively invoked concurrently on the elements of the original collection.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>collectManyParallel(Closure projection)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * <pre>
     * GParsPool.withPool {*     def squaresAndCubesOfOdds = [1, 2, 3, 4, 5].collectManyParallel { Number number -&gt;
     *         number % 2 ? [number ** 2, number ** 3] : []
     * }*     assert squaresAndCubesOfOdds == [1, 1, 9, 27, 25, 125]
     * }* </pre>
     */
    public static <T> List<T> collectManyParallel(final Object collection, final Closure<Collection<? extends T>> projection) {
        return (List<T>) GParsPoolUtilHelper.createPA(collection, retrievePool()).withMapping(new ClosureMapper(new CallClosure(projection))).reduce(new ClosureReducer(SumClosure.getInstance()), null);
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withMapping() method using the supplied
     * <code>projection</code> closure as the transformation operation. The <code>projection</code> closure should return a
     * (possibly empty) collection of items which are subsequently flattened to produce a new collection.
     * The <code>projection</code> closure will be effectively invoked concurrently on the elements of the original collection.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>collectManyParallel(Closure projection)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * <pre>
     * GParsPool.withPool {*     def squaresAndCubesOfOdds = [1, 2, 3, 4, 5].collectManyParallel { Number number -&gt;
     *         number % 2 ? [number ** 2, number ** 3] : []
     * }*     assert squaresAndCubesOfOdds == [1, 1, 9, 27, 25, 125]
     * }* </pre>
     */
    public static <T> List<T> collectManyParallel(final Map collection, final Closure<Collection<? extends T>> projection) {
        return (List<T>) createPA(collection, retrievePool()).withMapping(new ClosureMapper(buildClosureForMaps(projection))).reduce(new ClosureReducer(SumClosure.getInstance()), null);
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
     * <pre>
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].findAllParallel {Number number -&gt; number > 3}*     assertEquals(new HashSet([4, 5]), result)
     * }* </pre>
     */
    public static <T> Collection<T> findAllParallel(final Collection<T> collection, final Closure cl) {
        return GParsPoolUtilHelper.findAllParallelPA(GParsPoolUtilHelper.createPAFromCollection(collection, retrievePool()), cl);
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
     * <pre>
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].findAllParallel {Number number -&gt; number > 3}*     assertEquals(new HashSet([4, 5]), result)
     * }* </pre>
     */
    public static Collection<Object> findAllParallel(final Object collection, final Closure cl) {
        return (Collection<Object>) GParsPoolUtilHelper.findAllParallelPA(GParsPoolUtilHelper.createPA(collection, retrievePool()), cl);
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
     * <code>
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].findAllParallel {Number number -&gt; number > 3}*     assertEquals(new HashSet([4, 5]), result)
     * }* </code>
     */
    public static <K, V> Map<K, V> findAllParallel(final Map<K, V> collection, final Closure cl) {
        return buildResultMap(GParsPoolUtilHelper.findAllParallelPA(createPA(collection, retrievePool()), buildClosureForMaps(cl)));
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
     * <pre>
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].findParallel {Number number -&gt; number > 3}*     assert (result in [4, 5])
     * }* </pre>
     */
    @SuppressWarnings("GroovyAssignabilityCheck")
    public static <T> T findParallel(final Collection<T> collection, final Closure cl) {
        return GParsPoolUtilHelper.findParallelPA(GParsPoolUtilHelper.createPAFromCollection(collection, retrievePool()), cl);
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
     * <pre>
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].findParallel {Number number -&gt; number > 3}*     assert (result in [4, 5])
     * }* </pre>
     */
    public static Object findParallel(final Object collection, final Closure cl) {
        return GParsPoolUtilHelper.findParallelPA(GParsPoolUtilHelper.createPA(collection, retrievePool()), cl);
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
     * <pre>
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].findParallel {Number number -&gt; number > 3}*     assert (result in [4, 5])
     * }* </pre>
     */
    public static <K, V> Map.Entry<K, V> findParallel(final Map<K, V> collection, final Closure cl) {
        return GParsPoolUtilHelper.findParallelPA(createPA(collection, retrievePool()), buildClosureForMaps(cl));
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
     * <pre>
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].findParallel {Number number -&gt; number > 3}*     assert (result in [4, 5])
     * }* </pre>
     */
    public static <T> T findAnyParallel(final Collection<T> collection, final Closure cl) {
        return GParsPoolUtilHelper.findAnyParallelPA(GParsPoolUtilHelper.createPAFromCollection(collection, retrievePool()), cl);
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
     * </pre>
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].findAnyParallel {Number number -&gt; number > 3}*     assert (result in [4, 5])
     * }* </pre>
     */
    public static Object findAnyParallel(final Object collection, final Closure cl) {
        return GParsPoolUtilHelper.findAnyParallelPA(GParsPoolUtilHelper.createPA(collection, retrievePool()), cl);
    }

    /**
     * Creates a Parallel Array out of the supplied map and invokes the withFilter() method using the supplied
     * closure as the filter predicate.
     * Unlike with the <i>find</i> method, findAnyParallel() does not guarantee
     * that the matching element with the lowest index is returned.
     * The findAnyParallel() method evaluates elements lazily and stops processing further elements of the collection once a match has been found.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a random value from the resulting Parallel Array.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>findParallel(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * Example:
     * <pre>
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].findAnyParallel {Number number -&gt; number > 3}*     assert (result in [4, 5])
     * }* </pre>
     */
    public static <K, V> Map.Entry<K, V> findAnyParallel(final Map<K, V> collection, final Closure cl) {
        return GParsPoolUtilHelper.findAnyParallelPA(createPA(collection, retrievePool()), buildClosureForMaps(cl));
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
     * <pre>
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].grepParallel(4..6)
     *     assertEquals(new HashSet([4, 5]), result)
     * }* </pre>
     */
    public static <T> Collection<T> grepParallel(final Collection<T> collection, final Object filter) {
        return GParsPoolUtilHelper.grepParallelPA(GParsPoolUtilHelper.createPAFromCollection(collection, retrievePool()), filter);
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
     * <pre>
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].grepParallel(4..6)
     *     assertEquals(new HashSet([4, 5]), result)
     * }* </pre>
     */
    public static Object grepParallel(final Object collection, final Object filter) {
        return (Collection<Object>) GParsPoolUtilHelper.grepParallelPA(GParsPoolUtilHelper.createPA(collection, retrievePool()), filter);
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
     * <pre>
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].grepParallel(4..6)
     *     assertEquals(new HashSet([4, 5]), result)
     * }* </pre>
     */
    public static <K, V> Map<K, V> grepParallel(final Map<K, V> collection, final Object filter) {
        return buildResultMap(GParsPoolUtilHelper.grepParallelPA(createPA(collection, retrievePool()), filter instanceof Closure ? buildClosureForMaps((Closure<Object>) filter) : filter));
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
    public static <T> Collection<T> splitParallel(final Collection<T> collection, final Object filter) {
        final Map groups = groupByParallel(collection, (Closure) filter);
//        (Collection<T>) [groups[true] ?: [], groups[false] ?: []]
        return (Collection<T>) asList(groups.containsKey(Boolean.TRUE) ? groups.get(Boolean.TRUE) : new ArrayList<T>(), groups.containsKey(Boolean.FALSE) ? groups.get(Boolean.FALSE) : new ArrayList<T>());
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
     * <pre>
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].splitParallel(4..6)
     *     assert [3, 4, 5] as Set == result[0] as Set
     *     assert [1, 2] as Set == result[1] as Set
     * }* </pre>
     */
    public static Object splitParallel(final Object collection, final Object filter) {
        final Map groups = groupByParallelPA(GParsPoolUtilHelper.createPA(collection, retrievePool()), (Closure) filter);
        return asList(groups.containsKey(Boolean.TRUE) ? groups.get(Boolean.TRUE) : new ArrayList<Object>(), groups.containsKey(Boolean.FALSE) ? groups.get(Boolean.FALSE) : new ArrayList<Object>());
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
     * <pre>
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].countParallel(4)
     *     assertEquals(1, result)
     * }* </pre>
     */
    public static int countParallel(final Collection collection, final Object filter) {
        return GParsPoolUtilHelper.createPAFromCollection(collection, retrievePool()).withFilter(new Ops.Predicate<Object>() {
            @Override
            public boolean op(final Object o) {
                return filter.equals(o);
            }
        }).size();
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
     * <pre>
     * GParsPool.withPool {*     def result = [1, 2, 3, 4, 5].countParallel(4)
     *     assertEquals(1, result)
     * }* </pre>
     */
    public static int countParallel(final Object collection, final Object filter) {
        return GParsPoolUtilHelper.createPA(collection, retrievePool()).withFilter(new Ops.Predicate<Object>() {
            @Override
            public boolean op(final Object o) {
                return filter.equals(o);
            }
        }).size();
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
     * <pre>
     * GParsPool.withPool {*     def isOdd = { it % 2 }*     def result = [1, 2, 3, 4, 5].countParallel(isOdd)
     *     assert result == 3
     * }* </pre>
     */
    public static int countParallel(final Collection collection, final Closure filter) {
        return GParsPoolUtilHelper.createPAFromCollection(collection, retrievePool()).withFilter(new ClosurePredicate(filter)).size();
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
     * <pre>
     * GParsPool.withPool {*     def isEven = { it % 2 == 0 }*     def result = [1, 2, 3, 4, 5].countParallel(isEven)
     *     assert result == 2
     * }* </pre>
     */
    public static int countParallel(final Object collection, final Closure filter) {
        return GParsPoolUtilHelper.createPA(collection, retrievePool()).withFilter(new ClosurePredicate(filter)).size();
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
     * <pre>
     * GParsPool.withPool {*     assert [1, 2, 3, 4, 5].anyParallel {Number number -&gt; number > 3}*     assert ![1, 2, 3].anyParallel {Number number -&gt; number > 3}*}* </pre>
     */
    public static boolean anyParallel(final Collection collection, final Closure cl) {
        return GParsPoolUtilHelper.createPAFromCollection(collection, retrievePool()).withFilter(new ClosurePredicate(cl)).any() != null;
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
     * <pre>
     * GParsPool.withPool {*     assert [1, 2, 3, 4, 5].anyParallel {Number number -&gt; number > 3}*     assert ![1, 2, 3].anyParallel {Number number -&gt; number > 3}*}* </pre>
     */
    public static boolean anyParallel(final Object collection, final Closure cl) {
        return GParsPoolUtilHelper.createPA(collection, retrievePool()).withFilter(new ClosurePredicate(cl)).any() != null;
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
     * <pre>
     * GParsPool.withPool {*     assert [1, 2, 3, 4, 5].anyParallel {Number number -&gt; number > 3}*     assert ![1, 2, 3].anyParallel {Number number -&gt; number > 3}*}* </pre>
     */
    public static boolean anyParallel(final Map collection, final Closure cl) {
        final Closure mapClosure = buildClosureForMaps(cl);
        return createPA(collection, retrievePool()).withFilter(new ClosurePredicate(mapClosure)).any() != null;
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
     * <pre>
     * GParsPool.withPool(5) {*     assert ![1, 2, 3, 4, 5].everyParallel {Number number -&gt; number > 3}*     assert [1, 2, 3].everyParallel() {Number number -&gt; number <= 3}*}* </pre>
     */
    public static boolean everyParallel(final Collection collection, final Closure cl) {
        return GParsPoolUtilHelper.createPAFromCollection(collection, retrievePool()).withFilter(new ClosureNegationPredicate(cl)).any() == null;
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
     * <pre>
     * GParsPool.withPool(5) {*     assert ![1, 2, 3, 4, 5].everyParallel {Number number -&gt; number > 3}*     assert [1, 2, 3].everyParallel() {Number number -&gt; number <= 3}*}* </pre>
     */
    public static boolean everyParallel(final Object collection, final Closure cl) {
        return GParsPoolUtilHelper.createPA(collection, retrievePool()).withFilter(new ClosureNegationPredicate(cl)).any() == null;
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
     * <pre>
     * GParsPool.withPool(5) {*     assert ![1, 2, 3, 4, 5].everyParallel {Number number -&gt; number > 3}*     assert [1, 2, 3].everyParallel() {Number number -&gt; number <= 3}*}* </pre>
     */
    public static boolean everyParallel(final Map collection, final Closure cl) {
        final Closure mapClosure = buildClosureForMaps(cl);
        return createPA(collection, retrievePool()).withFilter(new ClosureNegationPredicate(mapClosure)).any() == null;
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
     * <pre>
     * GParsPool.withPool {*     assert ([1, 2, 3, 4, 5].groupByParallel {Number number -&gt; number % 2}).size() == 2
     * }* </pre>
     */
    public static <K, T> Map<K, List<T>> groupByParallel(final Collection<T> collection, final Closure<K> cl) {
        return groupByParallelPA(GParsPoolUtilHelper.createPAFromCollection(collection, retrievePool()), cl);
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
     * <pre>
     * GParsPool.withPool {*     assert ([1, 2, 3, 4, 5].groupByParallel {Number number -&gt; number % 2}).size() == 2
     * }* </pre>
     */
    public static <K> Map<K, List<Object>> groupByParallel(final Object collection, final Closure<K> cl) {
        return groupByParallelPA(GParsPoolUtilHelper.createPA(collection, retrievePool()), cl);
    }

    private static <K, T> Map<K, List<T>> groupByParallelPA(final ParallelArray<T> pa, final Closure<K> cl) {
        final ConcurrentHashMap<K, List<T>> map = new ConcurrentHashMap<K, List<T>>();
        GParsPoolUtilHelper.eachParallelPA(pa, createGroupByClosure(cl, map));
        return map;

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
     *
     * @param cl A one or two-argument closure
     */
    @SuppressWarnings("GroovyAssignabilityCheck")
    public static <T> T minParallel(final Collection<T> collection, final Closure cl) {
        return GParsPoolUtilHelper.createPAFromCollection(collection, retrievePool()).min((Comparator<T>) createComparator(cl));
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
     *
     * @param cl A one or two-argument closure
     */
    public static Object minParallel(final Object collection, final Closure cl) {
        return GParsPoolUtilHelper.createPA(collection, retrievePool()).min(createComparator(cl));
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its min() method using the default comparator.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the minimum of the elements in the collection.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     */
    @SuppressWarnings("GroovyAssignabilityCheck")
    public static <T> T minParallel(final Collection<T> collection) {
        return GParsPoolUtilHelper.createPAFromCollection(collection, retrievePool()).min();
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its min() method using the default comparator.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the minimum of the elements in the collection.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     */
    public static Object minParallel(final Object collection) {
        return GParsPoolUtilHelper.createPA(collection, retrievePool()).min();
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its max() method using the supplied
     * closure as the comparator.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the maximum of the elements in the collection.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>max(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * If the supplied closure takes two arguments it is used directly as a comparator.
     * If the supplied closure takes one argument, the values returned by the supplied closure for individual elements are used for comparison by the implicit comparator.
     *
     * @param cl A one or two-argument closure
     */
    @SuppressWarnings("GroovyAssignabilityCheck")
    public static <T> T maxParallel(final Collection<T> collection, final Closure cl) {
        return GParsPoolUtilHelper.createPAFromCollection(collection, retrievePool()).max((Comparator<T>) createComparator(cl));
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its max() method using the supplied
     * closure as the comparator.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the maximum of the elements in the collection.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>max(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     * If the supplied closure takes two arguments it is used directly as a comparator.
     * If the supplied closure takes one argument, the values returned by the supplied closure for individual elements are used for comparison by the implicit comparator.
     *
     * @param cl A one or two-argument closure
     */
    public static Object maxParallel(final Object collection, final Closure cl) {
        return GParsPoolUtilHelper.createPA(collection, retrievePool()).max(createComparator(cl));
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its max() method using the default comparator.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the maximum of the elements in the collection.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>max(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     */
    @SuppressWarnings("GroovyAssignabilityCheck")
    public static <T> T maxParallel(final Collection<T> collection) {
        return GParsPoolUtilHelper.createPAFromCollection(collection, retrievePool()).max();
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its max() method using the default comparator.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the maximum of the elements in the collection.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>max(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     */
    public static Object maxParallel(final Object collection) {
        return GParsPoolUtilHelper.createPA(collection, retrievePool()).max();
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and summarizes its elements using the foldParallel()
     * method with the + operator and the reduction operation.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the sum of the elements in the collection.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>sun(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     */
    public static <T> T sumParallel(final Collection<T> collection) {
        return foldParallel(collection, SumClosure.getInstance());
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and summarizes its elements using the foldParallel()
     * method with the + operator and the reduction operation.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the sum of the elements in the collection.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>sum(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     */
    public static Object sumParallel(final Object collection) {
        return foldParallel(collection, SumClosure.getInstance());
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its reduce() method using the supplied
     * closure as the reduction operation.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the reduction result of the elements in the collection.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>reduce(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     */
    @SuppressWarnings("GroovyAssignabilityCheck")
    public static <T> T foldParallel(final Collection<T> collection, final Closure cl) {
        return GParsPoolUtilHelper.createPAFromCollection(collection, retrievePool()).reduce(new ClosureReducer<T>(cl), null);
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its reduce() method using the supplied
     * closure as the reduction operation.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the reduction result of the elements in the collection.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>reduce(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     */
    public static Object foldParallel(final Object collection, final Closure cl) {
        return GParsPoolUtilHelper.createPA(collection, retrievePool()).reduce(new ClosureReducer(cl), null);
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its reduce() method using the supplied
     * closure as the reduction operation.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the reduction result of the elements in the collection.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>reduce(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     *
     * @param seed A seed value to initialize the operation
     */
    public static <T> T foldParallel(final Collection<T> collection, final T seed, final Closure cl) {
        return GParsPoolUtilHelper.foldParallel(collection, seed, cl);
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its reduce() method using the supplied
     * closure as the reduction operation.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the reduction result of the elements in the collection.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withPool</i> block
     * have a new <i>reduce(Closure cl)</i> method, which delegates to the <i>GParsPoolUtil</i> class.
     *
     * @param seed A seed value to initialize the operation
     */
    public static Object foldParallel(final Object collection, final Object seed, final Closure cl) {
        return GParsPoolUtilHelper.foldParallel(collection, seed, cl);
    }

    /**
     * Creates a PAWrapper around a ParallelArray wrapping the elements of the original collection.
     * This allows further parallel processing operations on the collection to chain and so effectively leverage the underlying
     * ParallelArray implementation.
     */
    public static <T> PAWrapper<T> getParallel(final Collection<T> collection) {
        return new PAWrapper(GParsPoolUtilHelper.createPAFromCollection(collection, retrievePool()));
    }

    /**
     * Creates a PAWrapper around a ParallelArray wrapping the elements of the original collection.
     * This allows further parallel processing operations on the collection to chain and so effectively leverage the underlying
     * ParallelArray implementation.
     */
    public static PAWrapper getParallel(final Object collection) {
        return new PAWrapper(GParsPoolUtilHelper.createPA(collection, retrievePool()));
    }

    /**
     * Creates a ParallelArray wrapping the elements of the original collection.
     */
    @SuppressWarnings("GroovyAssignabilityCheck")
    public static <T> ParallelArray<T> getParallelArray(final Collection<T> collection) {
        return GParsPoolUtilHelper.createPAFromCollection(collection, retrievePool());
    }

    /**
     * Creates a ParallelArray wrapping the elements of the original collection.
     */
    public static ParallelArray getParallelArray(final Object collection) {
        return GParsPoolUtilHelper.createPA(collection, retrievePool());
    }
}
