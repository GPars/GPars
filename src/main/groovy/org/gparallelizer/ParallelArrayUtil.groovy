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

import jsr166y.forkjoin.ForkJoinExecutor
import jsr166y.forkjoin.Ops.Predicate
import jsr166y.forkjoin.ParallelArray
import jsr166y.forkjoin.ForkJoinPool
import jsr166y.forkjoin.Ops.Mapper

/**
 * This class forms the core of the DSL initialized by <i>Parallelizer</i>. The static methods of <i>ParallelArrayUtil</i>
 * get attached to their first arguments (the Groovy Category mechanism) and can be then invoked as if they were part of
 * the argument classes.
 * @see org.gparallelizer.Parallelizer
 *
 * @author Vaclav Pech
 * Date: Oct 23, 2008
 */
public class ParallelArrayUtil {

    private static ForkJoinPool retrievePool() {
        final ForkJoinPool pool = Parallelizer.retrieveCurrentPool()
        if (pool==null) throw new IllegalStateException("No ForkJoinPool available for the current thread")
        return pool
    }

    private static <T> ParallelArray<T> createPA(Collection<T> collection, ForkJoinExecutor pool) {
        return ParallelArray.createFromCopy(collection.toArray(new Object[collection.size()]), pool)

//todo should be using generics, but groovyc blows on that
//        return ParallelArray.createFromCopy(collection.toArray(new T[collection.size()]), pool)
    }

    static java.util.Collection createCollection(Object object) {
        def collection = []
        for(element in object) collection << element
        return collection
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withMapping() method using the supplied
     * closure as the transformation operation.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>eachAsync(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     * Example:
     * Parallelizer.withParallelizer {
     *     def result = new ConcurrentSkipListSet()
     *     [1, 2, 3, 4, 5].eachAsync {Number number -> result.add(number * 10)}
     *     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     * }
     * Note that the <i>result</i> variable is synchronized to prevent race conditions between multiple threads.
     */
    public static <T> Collection<T> eachAsync(Collection<T> collection, Closure cl) {
        createPA(collection, retrievePool()).withMapping({cl(it)} as Mapper).all()
        return collection
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withMapping() method using the supplied
     * closure as the transformation operation.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>eachAsync(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     * Example:
     * Parallelizer.withParallelizer {
     *     def result = new ConcurrentSkipListSet()
     *     [1, 2, 3, 4, 5].eachAsync {Number number -> result.add(number * 10)}
     *     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     * }
     * Note that the <i>result</i> variable is synchronized to prevent race conditions between multiple threads.
     */
    public static Object eachAsync(Object collection, Closure cl) {
        eachAsync(createCollection(collection), cl)
        return collection
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withMapping() method using the supplied
     * closure as the transformation operation.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a collection of valuea from the resulting Parallel Array.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>collectAsync(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     * Example:
     * Parallelizer.withParallelizer {
     *     def result = [1, 2, 3, 4, 5].collectAsync {Number number -> number * 10}
     *     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     * }
     */
    public static <T> Collection<T> collectAsync(Collection<T> collection, Closure cl) {
        createPA(collection, retrievePool()).withMapping({cl(it)} as Mapper).all().asList()
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withMapping() method using the supplied
     * closure as the transformation operation.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a collection of valuea from the resulting Parallel Array.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>collectAsync(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     * Example:
     * Parallelizer.withParallelizer {
     *     def result = [1, 2, 3, 4, 5].collectAsync {Number number -> number * 10}
     *     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     * }
     */
    public static Collection<Object> collectAsync(Object collection, Closure cl) {
        return collectAsync(createCollection(collection), cl)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withFilter() method using the supplied
     * closure as the filter predicate.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a collection of valuea from the resulting Parallel Array.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>findAllAsync(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     * Example:
     * Parallelizer.withParallelizer {
     *     def result = [1, 2, 3, 4, 5].findAllAsync {Number number -> number > 3}
     *     assertEquals(new HashSet([4, 5]), result)
     * }
     */
    public static <T> Collection<T> findAllAsync(Collection<T> collection, Closure cl) {
        createPA(collection, retrievePool()).withFilter({cl(it)} as Predicate).all().asList()
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withFilter() method using the supplied
     * closure as the filter predicate.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a collection of valuea from the resulting Parallel Array.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>findAllAsync(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     * Example:
     * Parallelizer.withParallelizer {
     *     def result = [1, 2, 3, 4, 5].findAllAsync {Number number -> number > 3}
     *     assertEquals(new HashSet([4, 5]), result)
     * }
     */
    public static Collection<Object> findAllAsync(Object collection, Closure cl) {
        return findAllAsync(createCollection(collection), cl)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withFilter() method using the supplied
     * closure as the filter predicate.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a random value from the resulting Parallel Array.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>findAsync(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     * Example:
     * Parallelizer.withParallelizer {
     *     def result = [1, 2, 3, 4, 5].findAsync {Number number -> number > 3}
     *     assert (result in [4, 5])
     * }
     */
    public static <T> Object findAsync(Collection<T> collection, Closure cl) {
        //todo should return T, but gmaven rejects it
        createPA(collection, retrievePool()).withFilter({cl(it)} as Predicate).any()
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withFilter() method using the supplied
     * closure as the filter predicate.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a random value from the resulting Parallel Array.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>findAsync(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     * Example:
     * Parallelizer.withParallelizer {
     *     def result = [1, 2, 3, 4, 5].findAsync {Number number -> number > 3}
     *     assert (result in [4, 5])
     * }
     */
    public static Object findAsync(Object collection, Closure cl) {
        return findAsync(createCollection(collection), cl)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withFilter() method using the supplied
     * closure as the filter predicate.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a boolean value indicating, whenther at least
     * one element of the collection meets the predicate.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>anyAsync(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     * Example:
     * Parallelizer.withParallelizer {
     *     assert [1, 2, 3, 4, 5].anyAsync {Number number -> number > 3}
     *     assert ![1, 2, 3].anyAsync {Number number -> number > 3}
     * }
     */
    public static <T> boolean anyAsync(Collection<T> collection, Closure cl) {
        createPA(collection, retrievePool()).withFilter({cl(it)} as Predicate).any() != null
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withFilter() method using the supplied
     * closure as the filter predicate.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a boolean value indicating, whenther at least
     * one element of the collection meets the predicate.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>anyAsync(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     * Example:
     * Parallelizer.withParallelizer {
     *     assert [1, 2, 3, 4, 5].anyAsync {Number number -> number > 3}
     *     assert ![1, 2, 3].anyAsync {Number number -> number > 3}
     * }
     */
    public static boolean anyAsync(Object collection, Closure cl) {
        return anyAsync(createCollection(collection), cl)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withFilter() method using the supplied
     * closure as the filter predicate.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a boolean value indicating, whenther all the elements
     * of the collection meet the predicate.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>allAsync(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     * Example:
     * Parallelizer.withParallelizer(5) {
     *     assert ![1, 2, 3, 4, 5].allAsync {Number number -> number > 3}
     *     assert [1, 2, 3].allAsync() {Number number -> number <= 3}
     * }
     */
    public static <T> boolean allAsync(Collection<T> collection, Closure cl) {
        createPA(collection, retrievePool()).withFilter({cl(it)} as Predicate).all().size() == collection.size()
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withFilter() method using the supplied
     * closure as the filter predicate.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a boolean value indicating, whenther all the elements
     * of the collection meet the predicate.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>allAsync(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     * Example:
     * Parallelizer.withParallelizer(5) {
     *     assert ![1, 2, 3, 4, 5].allAsync {Number number -> number > 3}
     *     assert [1, 2, 3].allAsync() {Number number -> number <= 3}
     * }
     */
    public static boolean allAsync(Object collection, Closure cl) {
        return allAsync(createCollection(collection), cl)
    }
}
