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
 * The pool itself is stored in a final property threadPool and can be managed through static methods
 * on the ParallelEnhancer class.
 * All enhanced classes and instances will share the underlying pool. Use the getThreadPool() method to get hold of the thread pool.
 *
 * @author Vaclav Pech
 * Date: Jun 15, 2009
 */
//todo javadoc
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
    public boolean allParallel(Closure cl) {
        Parallelizer.ensurePool(ParallelEnhancer.threadPool.forkJoinPool) {
            ParallelArrayUtil.allParallel(mixedIn[Object], cl)
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
    private def enhance(Object collection) {
        ParallelEnhancer.enhanceInstance(collection)
    }
}