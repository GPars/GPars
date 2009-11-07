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

import java.util.concurrent.ConcurrentHashMap
import jsr166y.forkjoin.ForkJoinExecutor
import jsr166y.forkjoin.ForkJoinPool
import jsr166y.forkjoin.Ops.Mapper
import jsr166y.forkjoin.Ops.Predicate
import jsr166y.forkjoin.Ops.Reducer
import jsr166y.forkjoin.ParallelArray

/**
 * This class forms the core of the DSL initialized by <i>Parallelizer</i>. The static methods of <i>ParallelArrayUtil</i>
 * get attached to their first arguments (the Groovy Category mechanism) and can be then invoked as if they were part of
 * the argument classes.
 * @see groovyx.gpars.Parallelizer
 *
 * @author Vaclav Pech
 * Date: Oct 23, 2008
 */
public class ParallelArrayUtil {

    private static ForkJoinPool retrievePool() {
        final ForkJoinPool pool = Parallelizer.retrieveCurrentPool()
        if (pool == null) throw new IllegalStateException("No ForkJoinPool available for the current thread")
        return pool
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
     * overrides the iterative methods like each(), collect() and such, so that they call their parallel variants from the ParallelArrayUtil class
     * like eachParallel(), collectParallel() and such.
     * After mixing-in, the isTransparent() method will return true.
     * @param collection The object to make transparent
     * @return The instance of the TransparentParallel class wrapping the original object and overriding the iterative methods with new parallel behavior
     */
    public static Object makeTransparent(Object collection) {
        if (!(collection.respondsTo('isTransparent'))) throw new IllegalStateException("Cannot make the object transparently parallel. Apparently we're not inside a Parallelizer.doParallel() block nor the collection hasn't been enhanced with ParallelEnhancer.enhance().")
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
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>eachParallel(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     * Example:
     * Parallelizer.withParallelizer {*     def result = new ConcurrentSkipListSet()
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
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>eachParallel(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     * Example:
     * Parallelizer.withParallelizer {*     def result = new ConcurrentSkipListSet()
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
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>eachWithIndexParallel(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     * Example:
     * Parallelizer.withParallelizer {*     def result = new ConcurrentSkipListSet()
     *     [1, 2, 3, 4, 5].eachWithIndexParallel {Number number, int index -> result.add(number * 10)}*     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     *}* Note that the <i>result</i> variable is synchronized to prevent race conditions between multiple threads.
     */
    public static <T> Collection<T> eachWithIndexParallel(Collection<T> collection, Closure cl) {
        def indexedCollection = []
        int index = 0
        for(element in collection) {
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
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>eachWithIndexParallel(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     * Example:
     * Parallelizer.withParallelizer {*     def result = new ConcurrentSkipListSet()
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
     * After all the elements have been processed, the method returns a collection of valuea from the resulting Parallel Array.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>collectParallel(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     * Example:
     * Parallelizer.withParallelizer {*     def result = [1, 2, 3, 4, 5].collectParallel {Number number -> number * 10}*     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     *}*/
    public static <T> Collection<T> collectParallel(Collection<T> collection, Closure cl) {
        createPA(collection, retrievePool()).withMapping({cl(it)} as Mapper).all().asList()
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withMapping() method using the supplied
     * closure as the transformation operation.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a collection of valuea from the resulting Parallel Array.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>collectParallel(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     * Example:
     * Parallelizer.withParallelizer {*     def result = [1, 2, 3, 4, 5].collectParallel {Number number -> number * 10}*     assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
     *}*/
    public static Collection<Object> collectParallel(Object collection, Closure cl) {
        return collectParallel(createCollection(collection), cl)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withFilter() method using the supplied
     * closure as the filter predicate.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a collection of valuea from the resulting Parallel Array.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>findAllParallel(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     * Example:
     * Parallelizer.withParallelizer {*     def result = [1, 2, 3, 4, 5].findAllParallel {Number number -> number > 3}*     assertEquals(new HashSet([4, 5]), result)
     *}*/
    public static <T> Collection<T> findAllParallel(Collection<T> collection, Closure cl) {
        createPA(collection, retrievePool()).withFilter({cl(it)} as Predicate).all().asList()
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withFilter() method using the supplied
     * closure as the filter predicate.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a collection of valuea from the resulting Parallel Array.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>findAllParallel(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     * Example:
     * Parallelizer.withParallelizer {*     def result = [1, 2, 3, 4, 5].findAllParallel {Number number -> number > 3}*     assertEquals(new HashSet([4, 5]), result)
     *}*/
    public static Collection<Object> findAllParallel(Object collection, Closure cl) {
        return findAllParallel(createCollection(collection), cl)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withFilter() method using the supplied
     * closure as the filter predicate.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a random value from the resulting Parallel Array.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>findParallel(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     * Example:
     * Parallelizer.withParallelizer {*     def result = [1, 2, 3, 4, 5].findParallel {Number number -> number > 3}*     assert (result in [4, 5])
     *}*/
    public static <T> Object findParallel(Collection<T> collection, Closure cl) {
        createPA(collection, retrievePool()).withFilter({cl(it)} as Predicate).any()
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withFilter() method using the supplied
     * closure as the filter predicate.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a random value from the resulting Parallel Array.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>findParallel(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     * Example:
     * Parallelizer.withParallelizer {*     def result = [1, 2, 3, 4, 5].findParallel {Number number -> number > 3}*     assert (result in [4, 5])
     *}*/
    public static Object findParallel(Object collection, Closure cl) {
        return findParallel(createCollection(collection), cl)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withFilter() method using the supplied
     * rule as the filter predicate.
     * The filter will be effectively used concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a collection of valuea from the resulting Parallel Array.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>grepParallel(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     * Example:
     * Parallelizer.withParallelizer {*     def result = [1, 2, 3, 4, 5].grepParallel(4..6)
     *     assertEquals(new HashSet([4, 5]), result)
     *}*/
    public static <T> Collection<T> grepParallel(Collection<T> collection, filter) {
        createPA(collection, retrievePool()).withFilter({filter.isCase it} as Predicate).all().asList()
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withFilter() method using the supplied
     * rule as the filter predicate.
     * The filter will be effectively used concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a collection of valuea from the resulting Parallel Array.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>grepParallel(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     * Example:
     * Parallelizer.withParallelizer {*     def result = [1, 2, 3, 4, 5].grepParallel(4..6)
     *     assertEquals(new HashSet([4, 5]), result)
     *}*/
    public static Object grepParallel(Object collection, filter) {
        return grepParallel(createCollection(collection), filter)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withFilter() method using the supplied
     * closure as the filter predicate.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a boolean value indicating, whenther at least
     * one element of the collection meets the predicate.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>anyParallel(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     * Example:
     * Parallelizer.withParallelizer {*     assert [1, 2, 3, 4, 5].anyParallel {Number number -> number > 3}*     assert ![1, 2, 3].anyParallel {Number number -> number > 3}*}*/
    public static <T> boolean anyParallel(Collection<T> collection, Closure cl) {
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
     * have a new <i>anyParallel(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     * Example:
     * Parallelizer.withParallelizer {*     assert [1, 2, 3, 4, 5].anyParallel {Number number -> number > 3}*     assert ![1, 2, 3].anyParallel {Number number -> number > 3}*}*/
    public static boolean anyParallel(Object collection, Closure cl) {
        return anyParallel(createCollection(collection), cl)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withFilter() method using the supplied
     * closure as the filter predicate.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a boolean value indicating, whenther all the elements
     * of the collection meet the predicate.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>allParallel(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     * Example:
     * Parallelizer.withParallelizer(5) {*     assert ![1, 2, 3, 4, 5].allParallel {Number number -> number > 3}*     assert [1, 2, 3].allParallel() {Number number -> number <= 3}*}*/
    public static <T> boolean allParallel(Collection<T> collection, Closure cl) {
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
     * have a new <i>allParallel(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     * Example:
     * Parallelizer.withParallelizer(5) {*     assert ![1, 2, 3, 4, 5].allParallel {Number number -> number > 3}*     assert [1, 2, 3].allParallel() {Number number -> number <= 3}*}*/
    public static boolean allParallel(Object collection, Closure cl) {
        return allParallel(createCollection(collection), cl)
    }
    
    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withMapping() method using the supplied
     * closure as the mapping predicate.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a list of groups of the original elements.
     * Elements in the same group gave identical results when the supplied closure was invoked on them.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>groupByParallel(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     * Example:
     * Parallelizer.withParallelizer {*     assert ([1, 2, 3, 4, 5].groupByParallel {Number number -> number % 2}).size() == 2
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
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>groupByParallel(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     * Example:
     * Parallelizer.withParallelizer {*     assert ([1, 2, 3, 4, 5].groupByParallel {Number number -> number % 2}).size() == 2
     *}*/
    public static Map groupByParallel(Object collection, Closure cl) {
        return groupByParallel(createCollection(collection), cl)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its min() method using the supplied
     * closure as the comparator.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the minumum of the elements in the collection.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     */
    public static <T> T minParallel(Collection<T> collection, Closure cl) {
        createPA(collection, retrievePool()).min(cl as Comparator)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its min() method using the supplied
     * closure as the comparator.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the minumum of the elements in the collection.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     */
    public static Object minParallel(Object collection, Closure cl) {
        return minParallel(createCollection(collection), cl)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its min() method using the default comparator.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the minumum of the elements in the collection.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     */
    public static <T> T minParallel(Collection<T> collection) {
        createPA(collection, retrievePool()).min()
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its min() method using the default comparator.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the minumum of the elements in the collection.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
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
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     */
    public static <T> T maxParallel(Collection<T> collection, Closure cl) {
        createPA(collection, retrievePool()).max(cl as Comparator)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its max() method using the supplied
     * closure as the comparator.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the maximum of the elements in the collection.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     */
    public static Object maxParallel(Object collection, Closure cl) {
        return maxParallel(createCollection(collection), cl)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its max() method using the default comparator.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the maximum of the elements in the collection.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     */
    public static <T> T maxParallel(Collection<T> collection) {
        createPA(collection, retrievePool()).max()
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its max() method using the default comparator.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the maximum of the elements in the collection.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     */
    public static Object maxParallel(Object collection) {
        return maxParallel(createCollection(collection))
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and summarizes its elements using the reduceParallel()
     * method with the + operator and the reduction operation.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the sum of the elements in the collection.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     */
    public static <T> T sumParallel(Collection<T> collection) {
        reduceParallel(collection) {a, b -> a + b}
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and summarizes its elements using the reduceParallel()
     * method with the + operator and the reduction operation.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the sum of the elements in the collection.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
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
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     */
    public static <T> T reduceParallel(Collection<T> collection, Closure cl) {
        createPA(collection, retrievePool()).reduce(cl as Reducer, null)
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its reduce() method using the supplied
     * closure as the reduction operation.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the reduction result of the elements in the collection.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     */
    public static Object reduceParallel(Object collection, Closure cl) {
        return reduceParallel(createCollection(collection), cl)
    }

    /**
     * Creates a ParallelCollection around a ParallelArray wrapping te eleents of the original collection.
     * This allows further parallel processing operations on the collection to chain and so effectively leverage the underlying
     * ParallelArray implementation.
     */
    public static <T> ParallelCollection<T> getParallel(Collection<T> collection) {
        new ParallelCollection(createPA(collection, retrievePool()))
    }

    /**
     * Creates a ParallelCollection around a ParallelArray wrapping te eleents of the original collection.
     * This allows further parallel processing operations on the collection to chain and so effectively leverage the underlying
     * ParallelArray implementation.
     */
    public static Object getParallel(Object collection) {
        return getParallel(createCollection(collection))
    }
}

abstract class AbstractParallelCollection<T> {

    final def pa

    def AbstractParallelCollection(final pa) {
        this.pa = pa
    }

    public final Object getCollection() {
        this.pa.all()
    }

    public final T reduce(Closure cl) {
        pa.reduce(cl as Reducer, null)
    }

    public final T sum() {
        reduce{a, b -> a + b}
    }

    public final T size() {
        pa.size()
    }

    public final T min() {
        pa.min()
    }

    public final T min(Closure cl) {
        pa.min(cl as Comparator)
    }

    public final T max() {
        pa.max()
    }

    public final T max(Closure cl) {
        pa.max(cl as Comparator)
    }

    public final MappedCollection map(Closure cl) {
        new MappedCollection(pa.withMapping({cl(it)} as Mapper))
    }

    public abstract ParallelCollection filter(Closure cl)
}

final class ParallelCollection<T> extends AbstractParallelCollection {

    def ParallelCollection(final pa) {
        super(pa)
    }

    public ParallelCollection filter(Closure cl) {
        pa.withFilter({cl(it)} as Predicate).all().parallel
//        new FilterredCollection(pa.withFilter({cl(it)} as Predicate).all().parallel)
    }
}

final class MappedCollection<T> extends AbstractParallelCollection {

    def MappedCollection(final ParallelArray.WithMapping pa) {
        super(pa)
    }

    public ParallelCollection filter(Closure cl) {
        collection.parallel.filter(cl)
//        new FilterredCollection(pa.all().filter.withFilter({cl(it)} as Predicate))
    }

}

//final class FilterredCollection<T> extends AbstractParallelCollection {
//
//    def FilterredCollection(final ParallelArray.WithFilter pa) {
//        super(pa)
//    }
//
//    public FilterredCollection filter(Closure cl) {
//        new FilterredCollection(pa.withFilter({cl(it)} as Predicate))
//    }
//}
