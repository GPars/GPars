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

public class MemoizeAtLeastTest extends AbstractMemoizeTest {

    Closure buildMemoizeClosure(Closure cl) {
        cl.memoizeAtLeast(100)
    }

    public void testZeroCache() {
        groovyx.gpars.GParsPool.withPool {
            def flag = false
            Closure cl = {
                flag = true
                it * 2
            }
            Closure mem = cl.memoizeAtLeast(0)
            [1, 2, 3, 4, 5, 6].each {mem(it)}
            assert flag
        }
    }
}