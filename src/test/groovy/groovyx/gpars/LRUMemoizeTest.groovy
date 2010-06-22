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

package groovyx.gpars

/**
 * @author Vaclav Pech
 * Date: Jun 22, 2010
 */

public class LRUMemoizeTest extends AbstractMemoizeTest {

    def buildMemoizedClosure(Closure cl) {
        cl.memoize(100)
    }

    public void testLRUCaching() {
        groovyx.gpars.GParsPool.withPool(5) {
            def flag = false
            Closure cl = {
                flag = true
                it * 2
            }
            Closure mem = cl.memoize(5)
            [1, 2, 3, 4, 5, 6].each {println it; mem(it)}
            flag = false
            System.gc()
            sleep 2000
            assertEquals 4, mem(2)
            assertEquals 6, mem(3)
            assertEquals 12, mem(6)
            println 'AAAAAAAAAAAAAAAAAAAAAAAAaa'
            assert !flag

            System.gc()
            sleep 2000
            assertEquals 2, mem(1)
            println 'BBBBBBBBBBBBBBBBBBBBB'
            assert flag
            flag = false

            System.gc()
            sleep 2000
            assertEquals 4, mem(2)
            assertEquals 6, mem(3)
            assertEquals 12, mem(6)
            assertEquals 2, mem(1)
            assert !flag
            System.gc()
            sleep 2000
            assertEquals 8, mem(4)
            assert flag
        }
    }

}