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

package groovyx.gpars

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.Promise
import groovyx.gpars.scheduler.FJPool
import jsr166y.RecursiveAction

/**
 * @author Vaclav Pech
 */
public class GParsPoolAsyncFunTest extends GroovyTestCase {

    public void testFib() {
        groovyx.gpars.GParsPool.withPool(5) {
            def sum = {a, b -> a + b}.asyncFun()
            def fib
            fib = {n ->
                n <= 2 ? 1 : sum(fib(n - 2), fib(n - 1))
            }.asyncFun()

            assert fib(1).val == 1
            assert fib(10) instanceof Promise
            assert fib(10).get() == 55
            assert fib(10).val == 55
        }
    }

    public void testInject() {
        groovyx.gpars.GParsPool.withPool(5) {
            assert (0..100).inject(0, {a, b -> a + b}.asyncFun()).get() == 5050
            assert (0..100).inject(0, {a, b -> a + b}.asyncFun()).get() == 5050
            assert (0..1000).inject(0, {a, b -> a + b}.asyncFun()).val == 500500
        }
    }

    public void testCombining() {
        groovyx.gpars.GParsPool.withPool(5) {
            Closure sPlus = {Integer a, Integer b ->
                a + b
            }

            Closure sMultiply = {Integer a, Integer b ->
                a * b
            }

            Closure aPlus = sPlus.asyncFun()
            Closure aMultiply = sMultiply.asyncFun()

            assert sMultiply(sPlus(10, 30), 100) == aMultiply(aPlus(10, 30), 100).val
        }
    }

    public void testCombiningWithBlocking() {
        groovyx.gpars.GParsPool.withPool(5) {
            Closure sPlus = {Integer a, Integer b ->
                a + b
            }

            Closure sMultiply = {Integer a, Integer b ->
                a * b
            }

            Closure aPlus = sPlus.asyncFun()
            Closure aMultiply = sMultiply.asyncFun(true)

            assert sMultiply(sPlus(10, 30), 100) == aMultiply(aPlus(10, 30), 100)
        }
    }

    public void testThreading() {
        groovyx.gpars.GParsPool.withPool(1) {pool ->
            def results = new DataflowQueue()
            pool.submit([compute: {results << Thread.currentThread(); complete()}] as RecursiveAction)
            def t = results.val

            Closure sPlus = {Integer a, Integer b ->
                results << Thread.currentThread()
                a + b
            }

            Closure sMultiply = {Integer a, Integer b ->
                results << Thread.currentThread()
                a * b
            }

            Closure aPlus = sPlus.asyncFun()
            Closure aMultiply = sMultiply.asyncFun()

            aMultiply(aPlus(10, 30), 100).val
            assert results.val == t
            assert results.val == t
        }
    }

    public void testException() {
        groovyx.gpars.GParsPool.withPool(5) {
            Closure sPlus = {Integer a, Integer b ->
                if (a == -1) throw new RuntimeException('test')
                a + b
            }

            Closure sMultiply = {Integer a, Integer b ->
                if (a == -1) throw new RuntimeException('test')
                a * b
            }

            Closure aPlus = sPlus.asyncFun()
            Closure aMultiply = sMultiply.asyncFun()

            assert sMultiply(sPlus(10, 30), 100) == aMultiply(aPlus(10, 30), 100).val
            assert aMultiply(aPlus(-1, 30), 100).val instanceof RuntimeException
            assert aMultiply(aPlus(5, -6), 100).val instanceof RuntimeException
            shouldFail(RuntimeException) {
                assert aMultiply(aPlus(5, -6), 100).get()
            }
        }
    }

    public void testError() {
        groovyx.gpars.GParsPool.withPool(5) {
            Closure sPlus = {Integer a, Integer b ->
                if (a == -1) throw new Error('test')
                a + b
            }

            Closure sMultiply = {Integer a, Integer b ->
                if (a == -1) throw new Error('test')
                a * b
            }

            Closure aPlus = sPlus.asyncFun()
            Closure aMultiply = sMultiply.asyncFun()

            assert sMultiply(sPlus(10, 30), 100) == aMultiply(aPlus(10, 30), 100).val
            assert aMultiply(aPlus(-1, 30), 100).val instanceof Error
            assert aMultiply(aPlus(5, -6), 100).val instanceof Error
            shouldFail(Error) {
                assert aMultiply(aPlus(5, -6), 100).get()
            }
        }
    }

    public void testExplicitPool() {
        final pool = new FJPool()

        Closure sPlus = {Integer a, Integer b ->
            a + b
        }

        Closure sMultiply = {Integer a, Integer b ->
            a * b
        }

        Closure aPlus = GParsPoolUtil.asyncFun(sPlus, pool)
        Closure aMultiply = GParsPoolUtil.asyncFun(sMultiply, pool, true)

        assert sMultiply(sPlus(10, 30), 100) == aMultiply(aPlus(10, 30), 100)

    }

    public void testDelayedPool() {
        Closure sPlus = {Integer a, Integer b ->
            a + b
        }

        Closure sMultiply = {Integer a, Integer b ->
            a * b
        }

        Closure aPlus = GParsPoolUtil.asyncFun(sPlus)
        Closure aMultiply = GParsPoolUtil.asyncFun(sMultiply, true)

        groovyx.gpars.GParsPool.withPool(5) {
            assert sMultiply(sPlus(10, 30), 100) == aMultiply(aPlus(10, 30), 100)
        }

    }
}
