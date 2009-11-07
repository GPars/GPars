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
 * ParallelEnhancer allows classes or instances to be enhanced with parallel variants of iterative methods,
 * like eachParallel(), collectParallel(), findAllParallel() and others. These operations split processing into multiple
 * concurrently executable tasks and perform them on the underlying instance of the ForkJoinPool class from JSR-166y.
 * The Parallel class is mixed-in the enhanced instances or classes and delegates to the ParallelArrayUtil class
 * to perform the actual parallel implementation.
 * The collections returned from collect(), findAll() and grep() are again mixed with a Parallel instance,
 * so they also have the parallel iterative methods available on them.
 *
 * @author Vaclav Pech
 * Date: Nov 1, 2009
 */
final class Parallel {

    /**
     * Iterates over a collection/object with the <i>each()</i> method using an asynchronous variant of the supplied closure
     * to evaluate each collection's element.
     * After this method returns, all the closures have been finished and all the potential shared resources have been updated
     * by the threads.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * If any of the collection's elements causes the closure to throw an exception, the exception is rethrown.
     */
    public def eachParallel(Closure cl) {
        Parallelizer.ensurePool(ParallelEnhancer.threadPool.forkJoinPool) {
            ParallelArrayUtil.eachParallel(mixedIn[Object], cl)
        }
    }

    /**
     * Iterates over a collection/object with the <i>eachWithIndex()</i> method using an asynchronous variant of the supplied closure
     * to evaluate each collection's element.
     * After this method returns, all the closures have been finished and all the potential shared resources have been updated
     * by the threads.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * If any of the collection's elements causes the closure to throw an exception, the exception is rethrown.
     */
    public def eachWithIndexParallel(Closure cl) {
        Parallelizer.ensurePool(ParallelEnhancer.threadPool.forkJoinPool) {
            ParallelArrayUtil.eachWithIndexParallel(mixedIn[Object], cl)
        }
    }

    /**
     * Iterates over a collection/object with the <i>collect()</i> method using an asynchronous variant of the supplied closure
     * to evaluate each collection's element.
     * After this method returns, all the closures have been finished and the caller can safely use the result.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * If any of the collection's elements causes the closure to throw an exception, the exception is rethrown.
     * */
    public def collectParallel(Closure cl) {
        Parallelizer.ensurePool(ParallelEnhancer.threadPool.forkJoinPool) {
            enhance(ParallelArrayUtil.collectParallel(mixedIn[Object], cl))
        }
    }

    /**
     * Performs the <i>findAll()</i> operation using an asynchronous variant of the supplied closure
     * to evaluate each collection's/object's element.
     * After this method returns, all the closures have been finished and the caller can safely use the result.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * If any of the collection's elements causes the closure to throw an exception, the exception is rethrown.
     */
    public def findAllParallel(Closure cl) {
        Parallelizer.ensurePool(ParallelEnhancer.threadPool.forkJoinPool) {
            enhance(ParallelArrayUtil.findAllParallel(mixedIn[Object], cl))
        }
    }

    /**
     * Performs the <i>grep()</i> operation using an asynchronous variant of the supplied closure
     * to evaluate each collection's/object's element.
     * After this method returns, all the closures have been finished and the caller can safely use the result.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * If any of the collection's elements causes the closure to throw an exception, the exception is rethrown.
     */
    public def grepParallel(Closure cl) {
        Parallelizer.ensurePool(ParallelEnhancer.threadPool.forkJoinPool) {
            enhance(ParallelArrayUtil.grepParallel(mixedIn[Object], cl))
        }
    }

    /**
     * Performs the <i>find()</i> operation using an asynchronous variant of the supplied closure
     * to evaluate each collection's/object's element.
     * After this method returns, all the closures have been finished and the caller can safely use the result.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * If any of the collection's elements causes the closure to throw an exception, the exception is rethrown.
     */
    public def findParallel(Closure cl) {
        Parallelizer.ensurePool(ParallelEnhancer.threadPool.forkJoinPool) {
            ParallelArrayUtil.findParallel(mixedIn[Object], cl)
        }
    }

    /**
     * Performs the <i>all()</i> operation using an asynchronous variant of the supplied closure
     * to evaluate each collection's/object's element.
     * After this method returns, all the closures have been finished and the caller can safely use the result.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * If any of the collection's elements causes the closure to throw an exception, the exception is rethrown.
     */
    public boolean everyParallel(Closure cl) {
        Parallelizer.ensurePool(ParallelEnhancer.threadPool.forkJoinPool) {
            ParallelArrayUtil.everyParallel(mixedIn[Object], cl)
        }
    }

    /**
     * Performs the <i>any()</i> operation using an asynchronous variant of the supplied closure
     * to evaluate each collection's/object's element.
     * After this method returns, all the closures have been finished and the caller can safely use the result.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * If any of the collection's elements causes the closure to throw an exception, the exception is rethrown.
     */
    public boolean anyParallel(Closure cl) {
        Parallelizer.ensurePool(ParallelEnhancer.threadPool.forkJoinPool) {
            ParallelArrayUtil.anyParallel(mixedIn[Object], cl)
        }
    }

    /**
     * Performs the <i>groupBy()</i> operation using an asynchronous variant of the supplied closure
     * to evaluate each collection's/object's element.
     * After this method returns, all the closures have been finished and the caller can safely use the result.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * If any of the collection's elements causes the closure to throw an exception, the exception is rethrown.
     */
    public def groupByParallel(Closure cl) {
        Parallelizer.ensurePool(ParallelEnhancer.threadPool.forkJoinPool) {
            ParallelArrayUtil.groupByParallel(mixedIn[Object], cl)
        }
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its min() method using the supplied
     * closure as the comparator.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the minumum of the elements in the collection.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     * If the supplied closure takes two arguments it is used directly as a comparator.
     * If the supplied closure takes one argument, the values returned by the supplied closure for individual elements are used for comparison by the implicit comparator.
     * @param cl A one or two-argument closure
     */
    public def minParallel(Closure cl) {
        Parallelizer.ensurePool(ParallelEnhancer.threadPool.forkJoinPool) {
            ParallelArrayUtil.minParallel(mixedIn[Object], cl)
        }
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its min() method using the default comparator.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the minumum of the elements in the collection.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     */
    public def minParallel() {
        Parallelizer.ensurePool(ParallelEnhancer.threadPool.forkJoinPool) {
        ParallelArrayUtil.minParallel(mixedIn[Object])
        }
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its max() method using the supplied
     * closure as the comparator.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the maximum of the elements in the collection.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     * If the supplied closure takes two arguments it is used directly as a comparator.
     * If the supplied closure takes one argument, the values returned by the supplied closure for individual elements are used for comparison by the implicit comparator.
     * @param cl A one or two-argument closure
     */
    public def maxParallel(Closure cl) {
        Parallelizer.ensurePool(ParallelEnhancer.threadPool.forkJoinPool) {
            ParallelArrayUtil.maxParallel(mixedIn[Object], cl)
        }
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and invokes its max() method using the default comparator.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the maximum of the elements in the collection.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     */
    public def maxParallel() {
        Parallelizer.ensurePool(ParallelEnhancer.threadPool.forkJoinPool) {
            ParallelArrayUtil.maxParallel(mixedIn[Object])
        }
    }

    /**
     * Creates a Parallel Array out of the supplied collection/object and summarizes its elements using the foldParallel()
     * method with the + operator and the reduction operation.
     * The closure will be effectively invoked concurrently on the elements of the collection.
     * After all the elements have been processed, the method returns the sum of the elements in the collection.
     * Alternatively a DSL can be used to simplify the code. All collections/objects within the <i>withParallelizer</i> block
     * have a new <i>min(Closure cl)</i> method, which delegates to the <i>ParallelArrayUtil</i> class.
     */
    public def sumParallel() {
        Parallelizer.ensurePool(ParallelEnhancer.threadPool.forkJoinPool) {
            ParallelArrayUtil.sumParallel(mixedIn[Object])
        }
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
    public def foldParallel(Closure cl) {
        Parallelizer.ensurePool(ParallelEnhancer.threadPool.forkJoinPool) {
            ParallelArrayUtil.foldParallel(mixedIn[Object], cl)
        }
    }

    /**
     * Creates a ParallelCollection around a ParallelArray wrapping te eleents of the original collection.
     * This allows further parallel processing operations on the collection to chain and so effectively leverage the underlying
     * ParallelArray implementation.
     */
    public ParallelCollection getParallel() {
        Parallelizer.ensurePool(ParallelEnhancer.threadPool.forkJoinPool) {
            ParallelArrayUtil.getParallel(mixedIn[Object])
        }
    }

    /**
     * Indicates, whether the iterative methods like each() or collect() have been made parallel.
     */
    public def boolean isTransparent() {return false}

    /**
     * Creates a TransparentParallel class instance and mixes it in the object it is invoked on. The TransparentParallel class
     * overrides the iterative methods like each(), collect() and such, so that they call their parallel variants from the ParallelArrayUtil class
     * like eachParallel(), collectParallel() and such.
     * After mixing-in, the isTransparent() method will return true.
     * Delegates to ParallelArrayUtil.makeTransparent().
     * @param collection The object to make transparent
     * @return The instance of the TransparentParallel class wrapping the original object and overriding the iterative methods with new parallel behavior
     */
    static Object makeTransparent(Object collection) {
        ParallelArrayUtil.makeTransparent(collection)
    }

    /**
     * Enhances to resulting collection so that parallel methods can be chained.
     */
    @SuppressWarnings("GroovyMultipleReturnPointsPerMethod")
    private static def enhance(Object collection) {
        ParallelEnhancer.enhanceInstance(collection)
    }
}