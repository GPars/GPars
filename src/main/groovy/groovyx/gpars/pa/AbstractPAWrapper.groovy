// GPars - Groovy Parallel Systems
//
// Copyright © 2008–2012, 2014  The original author or authors
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


package groovyx.gpars.pa

import groovyx.gpars.GParsPoolUtil

// import static groovyx.gpars.util.PAUtils.createComparator

/**
 * Wraps a ParallelArray instance in map/reduce operation chains.
 */
abstract class AbstractPAWrapper<T> {

    /**
     * The wrapper ParallelArray instance
     */
    final pa

    /**
     * Creates an instance wrapping the supplied instance of ParallelArray
     */
    AbstractPAWrapper(final pa) {
        this.pa = pa
    }

    /**
     * Reconstructs a collection from the wrapped ParallelArray instance
     * @return A collection containing all elements of the wrapped ParallelArray
     */
    final Object getCollection() {
        this.pa as ArrayList
    }

    /**
     * Performs a parallel reduce operation. It will use the supplied two-argument closure to gradually reduce two elements into one.
     * @param cl A two-argument closure merging two elements into one. The return value of the closure will replace the original two elements.
     * @return The product of reduction
     */
    final T reduce(final Closure cl) {
        return GParsPoolUtil.injectParallel(pa, cl)
    }

    /**
     * Performs a parallel reduce operation. It will use the supplied two-argument closure to gradually reduce two elements into one.
     * @param cl A two-argument closure merging two elements into one. The return value of the closure will replace the original two elements.
     * @return The product of reduction
     */
    final T reduce(seed, final Closure cl) {
        return GParsPoolUtil.injectParallel(pa, seed, cl) as T
    }

    /**
     * Summarizes all elements of the collection in parallel using the "plus()" operator of the elements
     * @return The summary od all elements in the collection
     */
    final T sum() {
        return GParsPoolUtil.sumParallel(pa) as T
    }

    /**
     * Size of the collection
     * @return The number of elements in the collection
     */
    final int size() {
        pa.size()
    }

    /**
     * Finds in parallel the minimum of all values in the collection. The implicit comparator is used.
     * @return The minimum element of the collection
     */
    final T min() {
        return GParsPoolUtil.minParallel(pa) as T
    }

    /**
     * Finds in parallel the minimum of all values in the collection. The supplied comparator is used.
     * If the supplied closure takes two arguments it is used directly as a comparator.
     * If the supplied closure takes one argument, the values returned by the supplied closure for individual elements are used for comparison by the implicit comparator.
     * @param cl A one or two-argument closure
     * @return The minimum element of the collection
     */
    final T min(final Closure cl) {
        return GParsPoolUtil.minParallel(pa, cl) as T
    }

    /**
     * Finds in parallel the maximum of all values in the collection. The implicit comparator is used.
     * @return The maximum element of the collection
     */
    final T max() {
        return GParsPoolUtil.maxParallel(pa) as T
    }

    /**
     * Finds in parallel the maximum of all values in the collection. The supplied comparator is used.
     * If the supplied closure takes two arguments it is used directly as a comparator.
     * If the supplied closure takes one argument, the values returned by the supplied closure for individual elements are used for comparison by the implicit comparator.
     * @param cl A one or two-argument closure
     * @return The maximum element of the collection
     */
    final T max(final Closure cl) {
        return GParsPoolUtil.maxParallel(pa, cl) as T
    }

    /**
     * Returns a sorted parallel collection
     * If the supplied closure takes two arguments it is used directly as a comparator.
     * If the supplied closure takes one argument, the values returned by the supplied closure for individual elements are used for comparison by the implicit comparator.
     * @param cl A one or two-argument closure
     * @return A sorted collection holding all the elements
     */
    final AbstractPAWrapper sort(final Closure cl = { it }) {
        return new PAWrapper(pa.sort(cl)) as AbstractPAWrapper
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
    Map groupBy(Closure cl) {
        return GParsPoolUtil.groupByParallel(pa, cl) as Map
    }

    /**
     * Applies concurrently the supplied function to all elements in the collection, returning a collection containing
     * the transformed values.
     * @param A closure calculating a transformed value from the original one
     * @return A collection holding the new values
     */
    final AbstractPAWrapper map(final Closure cl) {
        return new PAWrapper(GParsPoolUtil.collectParallel(pa, cl))
    }

    /**
     * Filters concurrently elements in the collection based on the outcome of the supplied function on each of the elements.
     * @param A closure indicating whether to propagate the given element into the filtered collection
     * @return A collection holding the allowed values
     */
    AbstractPAWrapper filter(final Closure cl) {
        return new PAWrapper(GParsPoolUtil.findAllParallel(pa, cl))
    }
}
