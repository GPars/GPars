// GPars - Groovy Parallel Systems
//
// Copyright © 2008–2011, 2014  The original author or authors
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
import java.util.concurrent.ConcurrentHashMap

/**
 * @author Vaclav Pech
 * Date: Nov 6, 2009
 */

public class ParallelStreamTest extends groovy.test.GroovyTestCase {

    public void testReduce() {
        GParsPool.withPool(5) {
            assert 15 == [1, 2, 3, 4, 5].parallelStream().map({it}).reduce({a, b -> a + b}).get()
        }
    }

    public void testNestedMap() {
        GParsPool.withPool(5) {
            assert 65 == [1, 2, 3, 4, 5].parallelStream().map({it + 10}).reduce({a, b -> a + b}).get()
        }
    }

    public void testReduceThreads() {
        final Map map = new ConcurrentHashMap()

        GParsPool.withPool(5) {
            assert 55 == [1, 2, 3, 4, 5, 6, 7, 8, 9, 10].parallelStream().reduce({a, b ->
                Thread.sleep 200
                map[Thread.currentThread()] = ''
                a + b
	    }).get()
            assert map.keys().size() > 1
        }
    }

    public void testMinMax() {
        GParsPool.withPool(5) {
            assert 1 == [1, 2, 3, 4, 5].parallelStream().min({a, b -> a - b} as Comparator).get()
            assert 5 == [1, 2, 3, 4, 5].parallelStream().max({a, b -> a - b} as Comparator).get()
        }
    }
}
