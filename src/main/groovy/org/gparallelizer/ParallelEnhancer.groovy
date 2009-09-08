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

import org.gparallelizer.ParallelArrayUtil
import org.gparallelizer.Parallelizer
import org.gparallelizer.actors.pooledActors.FJPool
import org.gparallelizer.actors.pooledActors.Pool

/**
 * ParallelEnhancer allows classes or instances to be enhanced with asynchronous variants of iterative methods,
 * like eachAsync(), collectAsync(), findAllAsync() and others. These operations split processing into multiple
 * concurrently executable tasks and perform them on the underlying instance of the ForkJoinPool class from JSR-166y.
 * The pool itself is stored in a final property threadPool and can be managed through static methods
 * on the ParallelEnhancer class.
 * All enhanced classes and instances will share the underlying pool.
 *
 * @author Vaclav Pech
 * Date: Jun 15, 2009
 */
public final class ParallelEnhancer {

    /**
     * Holds the internal ForkJoinPool instance wrapped into a FJPool
     */
    private final static FJPool threadPool = new FJPool()

    /**
     * Enhances a single instance by mixing-in an instance of ParallelEnhancer.
     */
    public static void enhanceInstance(Object collection) {
        collection.getMetaClass().mixin ParallelEnhancer
    }

    /**
     * Enhances a class and so all instances created in the future by mixing-in an instance of ParallelEnhancer.
     * Enhancing classes needs to be done with caution, since it may have impact in unrelated parts of the application.
     */
    public static void enhanceClass(Class clazz) {
        clazz.getMetaClass().mixin ParallelEnhancer
    }

    /**
     * Retrieves the underlying pool
     */
    public Pool getThreadPool() { return threadPool }

    /**
     * Iterates over a collection/object with the <i>each()</i> method using an asynchronous variant of the supplied closure
     * to evaluate each collection's element.
     * After this method returns, all the closures have been finished and all the potential shared resources have been updated
     * by the threads.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * If any of the collection's elements causes the closure to throw an exception, the exception is rethrown.
     */
    public def eachAsync(Closure cl) {
        Parallelizer.withExistingParallelizer(threadPool.forkJoinPool) {
            ParallelArrayUtil.eachAsync(mixedIn[Object], cl)
        }
    }

    /**
     * Iterates over a collection/object with the <i>collect()</i> method using an asynchronous variant of the supplied closure
     * to evaluate each collection's element.
     * After this method returns, all the closures have been finished and the caller can safely use the result.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * If any of the collection's elements causes the closure to throw an exception, the exception is rethrown.
     *  */
    public def collectAsync(Closure cl) {
        Parallelizer.withExistingParallelizer(threadPool.forkJoinPool) {
            ParallelArrayUtil.collectAsync(mixedIn[Object], cl)
        }
    }

    /**
     * Performs the <i>findAll()</i> operation using an asynchronous variant of the supplied closure
     * to evaluate each collection's/object's element.
     * After this method returns, all the closures have been finished and the caller can safely use the result.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * If any of the collection's elements causes the closure to throw an exception, the exception is rethrown.
     */
    public def findAllAsync(Closure cl) {
        Parallelizer.withExistingParallelizer(threadPool.forkJoinPool) {
            ParallelArrayUtil.findAllAsync(mixedIn[Object], cl)
        }
    }

    /**
     * Performs the <i>find()</i> operation using an asynchronous variant of the supplied closure
     * to evaluate each collection's/object's element.
     * After this method returns, all the closures have been finished and the caller can safely use the result.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * If any of the collection's elements causes the closure to throw an exception, the exception is rethrown.
     */
    public def findAsync(Closure cl) {
        Parallelizer.withExistingParallelizer(threadPool.forkJoinPool) {
            ParallelArrayUtil.findAsync(mixedIn[Object], cl)
        }
    }

    /**
     * Performs the <i>all()</i> operation using an asynchronous variant of the supplied closure
     * to evaluate each collection's/object's element.
     * After this method returns, all the closures have been finished and the caller can safely use the result.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * If any of the collection's elements causes the closure to throw an exception, the exception is rethrown.
     */
    public boolean allAsync(Closure cl) {
        Parallelizer.withExistingParallelizer(threadPool.forkJoinPool) {
            ParallelArrayUtil.allAsync(mixedIn[Object], cl)
        }
    }

    /**
     * Performs the <i>any()</i> operation using an asynchronous variant of the supplied closure
     * to evaluate each collection's/object's element.
     * After this method returns, all the closures have been finished and the caller can safely use the result.
     * It's important to protect any shared resources used by the supplied closure from race conditions caused by multi-threaded access.
     * If any of the collection's elements causes the closure to throw an exception, the exception is rethrown.
     */
    public boolean anyAsync(Closure cl) {
        Parallelizer.withExistingParallelizer(threadPool.forkJoinPool) {
            ParallelArrayUtil.anyAsync(mixedIn[Object], cl)
        }
    }
}
