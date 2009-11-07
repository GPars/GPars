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

import java.util.concurrent.ExecutorService

/**
 * @author Vaclav Pech
 * Date: Nov 10, 2008
 */
public class AsynchronizerStringTest extends GroovyTestCase {

    public void testEachParallelWithString() {
        def result = Collections.synchronizedSet(new HashSet())
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            'abc'.eachParallel {result.add(it.toUpperCase())}
            assertEquals(new HashSet(['A', 'B', 'C']), result)
        }
    }

    public void testCollectParallelWithString() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            def result = 'abc'.collectParallel {it.toUpperCase()}
            assertEquals(['A', 'B', 'C'], result)
        }
    }

    public void testFindAllParallelWithString() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            def result = 'aBC'.findAllParallel {it == it.toUpperCase()}
            assertEquals(['B', 'C'], result)
        }
    }

    public void testFindParallelWithString() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            def result = 'aBC'.findParallel {it == it.toUpperCase()}
            assert result in ['B', 'C']
        }
    }

    public void testGrepParallelWithThreadPoolAndString() {
        Asynchronizer.withAsynchronizer(5) {
            def result = 'aBC'.grepParallel('B')
            assertEquals (['B'], result)
        }
    }

    public void testAllParallelWithString() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            assert 'ABC'.everyParallel {it == it.toUpperCase()}
            assert !'aBC'.everyParallel {it == it.toUpperCase()}
        }
    }

    public void testAnyParallelWithString() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            assert 'aBc'.anyParallel {it == it.toUpperCase()}
            assert 'aBC'.anyParallel {it == it.toUpperCase()}
            assert !'abc'.anyParallel {it == it.toUpperCase()}
        }
    }
}
