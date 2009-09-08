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

import java.util.concurrent.atomic.AtomicInteger
import jsr166y.forkjoin.ForkJoinPool
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.CyclicBarrier
import java.lang.Thread.UncaughtExceptionHandler
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author Vaclav Pech
 * Date: Oct 23, 2008
 */
public class ParallelUtilTest extends GroovyTestCase {

    public void testEach() {
        Parallelizer.withParallelizer(5) {
            final AtomicInteger result = new AtomicInteger(0)
            ParallelArrayUtil.eachAsync([1, 2, 3, 4, 5],  {result.addAndGet(it)})
            assertEquals 15, result
        }
    }

    public void testCollect() {
        Parallelizer.withParallelizer(5) {
            final List result = ParallelArrayUtil.collectAsync([1, 2, 3, 4, 5], {it * 2})
            assert ([2, 4, 6, 8, 10].equals(result))
            assert !([2, 41, 6, 8, 10].equals(result))
        }
    }

    public void testFindAll() {
        Parallelizer.withParallelizer(5) {
            final List result = ParallelArrayUtil.findAllAsync([1, 2, 3, 4, 5], {it > 2})
            assert !(1 in result)
            assert !(1 in result)
            assert (3 in result)
            assert (4 in result)
            assert (5 in result)
            assertEquals 3, result.size()
        }
    }

    public void testEmptyFindAll() {
        Parallelizer.withParallelizer(5) {
            final List<Integer> result = ParallelArrayUtil.findAllAsync([1, 2, 3, 4, 5], {it > 6})
            assertEquals 0, result.size()
        }
    }

    public void testFind() {
        Parallelizer.withParallelizer(5) {
            final int result = ParallelArrayUtil.findAsync([1, 2, 3, 4, 5], {it > 2})
            assert result in [3, 4, 5]
        }
    }

    public void testEmptyFind() {
        Parallelizer.withParallelizer(5) {
            final int result = ParallelArrayUtil.findAsync([1, 2, 3, 4, 5], {it > 6})
            assertNull result
        }
    }

    public void testAny() {
        Parallelizer.withParallelizer(5) {
            assert ParallelArrayUtil.anyAsync([1, 2, 3, 4, 5], {it > 2})
            assert !ParallelArrayUtil.anyAsync([1, 2, 3, 4, 5], {it > 6})
        }
    }

    public void testAll() {
        Parallelizer.withParallelizer(5) {
            assert ParallelArrayUtil.allAsync([1, 2, 3, 4, 5], {it > 0})
            assert !ParallelArrayUtil.allAsync([1, 2, 3, 4, 5], {it > 1})
        }
    }

    public void testParallelPools() {
        final AtomicReference reference = new AtomicReference()
        final CyclicBarrier barrier1 = new CyclicBarrier(2)
        final CyclicBarrier barrier2 = new CyclicBarrier(2)

        def thread=new Thread({
            Parallelizer.withParallelizer(2) {
                final ForkJoinPool pool2 = Parallelizer.retrieveCurrentPool()
                reference.set pool2
                barrier1.await()
                barrier2.await()
            }
        } as Runnable)
        thread.start()
        Parallelizer.withParallelizer(5) {
            final ForkJoinPool pool1 = Parallelizer.retrieveCurrentPool()
            barrier1.await()
            final ForkJoinPool nestedPool = reference.get() as ForkJoinPool
            assert pool1 != nestedPool
            barrier2.await()
        }
    }

    //todo exception handler doesn't seem to be accepted by the pool
    //todo try setting exception handlers on the thread level in both Parallelizer and Asynchronizer
    //todo consider queue and pool options
    //todo consider rejection policy
    public void testExceptionHandler() {
        final AtomicInteger exceptionCount = new AtomicInteger(0)
        def handler = {Thread t, Throwable e ->
            exceptionCount.incrementAndGet()
        } as UncaughtExceptionHandler

        shouldFail(RuntimeException.class) {
            Parallelizer.withParallelizer(5, handler) {ForkJoinPool pool ->
                'abc'.eachAsync{throw new RuntimeException('test')}
            }
        }
//        assertEquals 3, exceptionCount.get()
    }

    public testNestedCalls() {
        Parallelizer.withParallelizer(5) {pool ->
            def result = ['abc', '123', 'xyz'].findAllAsync {word ->
                Parallelizer.withExistingParallelizer(pool) {
                    word.anyAsync {it in ['a', 'y', '5']}
                }
            }
            assertEquals(['abc', 'xyz'], result)
        }
    }
}
