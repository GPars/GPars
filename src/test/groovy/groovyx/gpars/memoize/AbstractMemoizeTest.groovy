// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
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

package groovyx.gpars.memoize

/**
 * @author Vaclav Pech
 * Date: Jun 22, 2010
 */
public abstract class AbstractMemoizeTest extends GroovyTestCase {

    public void testCorrectness() {
        groovyx.gpars.GParsPool.withPool(5) {
            Closure cl = {it * 2}
            Closure mem = buildMemoizeClosure(cl)
            assertEquals 10, mem(5)
            assertEquals 4, mem(2)
        }
    }

    abstract Closure buildMemoizeClosure(Closure cl)

    public void testNullParams() {
        groovyx.gpars.GParsPool.withPool(5) {
            Closure cl = {2}
            Closure mem = cl.memoize()
            assertEquals 2, mem(5)
            assertEquals 2, mem(2)
            assertEquals 2, mem(null)
        }
    }

    public void testNoParams() {
        groovyx.gpars.GParsPool.withPool(5) {
            Closure cl = {-> 2}
            Closure mem = cl.memoize()
            assertEquals 2, mem()
            assertEquals 2, mem()
        }
    }

    public void testCaching() {
        groovyx.gpars.GParsPool.withPool(5) {
            def flag = false
            Closure cl = {
                flag = true
                it * 2
            }
            Closure mem = cl.memoize()
            assertEquals 10, mem(5)
            assert flag
            flag = false
            assertEquals 4, mem(2)
            assert flag
            flag = false

            assertEquals 4, mem(2)
            assertEquals 4, mem(2)
            assertEquals 10, mem(5)
            assert !flag

            assertEquals 6, mem(3)
            assert flag
            flag = false
            assertEquals 6, mem(3)
            assert !flag
        }
    }

    public void testComplexParameter() {
        def callFlag = []

        groovyx.gpars.GParsPool.withPool(5) {
            Closure cl = {a, b, c ->
                callFlag << true
                c
            }
            Closure mem = cl.memoize()
            checkParams(mem, callFlag, [1, 2, 3], 3)
            checkParams(mem, callFlag, [1, 2, 4], 4)
            checkParams(mem, callFlag, [1, [2], 4], 4)
            checkParams(mem, callFlag, [[1: '1'], [2], 4], 4)
            checkParams(mem, callFlag, [[1, 2], 2, 4], 4)
            checkParams(mem, callFlag, [[1, 2], null, 4], 4)
            checkParams(mem, callFlag, [null, null, 4], 4)
            checkParams(mem, callFlag, [null, null, null], null)
            checkParams(mem, callFlag, [null, [null], null], null)

        }
    }

    def checkParams(Closure mem, callFlag, args, desiredResult) {
        assertEquals desiredResult, mem(* args)
        assert !callFlag.empty
        callFlag.clear()
        assertEquals desiredResult, mem(* args)
        assert callFlag.empty
    }
}