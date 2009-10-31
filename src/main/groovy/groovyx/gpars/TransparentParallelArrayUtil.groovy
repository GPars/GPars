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

/**
 * This class forms the core of the DSL initialized by <i>Parallelizer</i>. The static methods of <i>ParallelArrayUtil</i>
 * get attached to their first arguments (the Groovy Category mechanism) and can be then invoked as if they were part of
 * the argument classes.
 * @see groovyx.gpars.Parallelizer
 *
 * @author Vaclav Pech
 * Date: Oct 30, 2008
 */

public class TransparentParallelArrayUtil {

    //todo update javadoc
    //todo test

    /**
     * Indicates whether the iterative methods like each() or collect() work have been altered to work concurrently.
     */
    public static boolean isTransparentlyParallel(Object collection) { true }

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
    public static Object each(Object collection, Closure cl) {
        return ParallelArrayUtil.eachParallel(collection, cl)
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
    public static Object eachWithIndex(Object collection, Closure cl) {
        return ParallelArrayUtil.eachwithIndexParallel(collection, cl)
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
    public static Collection<Object> collect(Object collection, Closure cl) {
        return ParallelArrayUtil.collectParallel(collection, cl)
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
    public static Collection<Object> findAll(Object collection, Closure cl) {
        return ParallelArrayUtil.findAllParallel(collection, cl)
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
    public static Object find(Object collection, Closure cl) {
        return ParallelArrayUtil.findParallel(collection, cl)
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
    public static Object grep(Object collection, filter) {
        return ParallelArrayUtil.grepParallel(collection, filter)
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
    public static boolean any(Object collection, Closure cl) {
        return ParallelArrayUtil.anyParallel(collection, cl)
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
    public static boolean all(Object collection, Closure cl) {
        return ParallelArrayUtil.allParallel(collection, cl)
    }
    
    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes the withMapping() method using the supplied
     * closure as the filter predicate.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns a list of groups of the original elements.
     * Elements in the same group gave identical results when the supplied closure was invoked on them.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>groupByParallel(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     * Example:
     * Parallelizer.withParallelizer {*     assert ([1, 2, 3, 4, 5].groupByParallel {Number number -> number % 2}).size() == 2
     *}*/
    public static List<Object> groupBy(Object collection, Closure cl) {
        return ParallelArrayUtil.groupByParallel(collection, cl)
    }
}