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
 * @author Vaclav Pech
 * Date: Oct 23, 2008
 */
public class ParallelizerStringTest extends GroovyTestCase {
    public void testEachParallelWithThreadPoolAndString() {
        Parallelizer.withParallelizer(5) {
            def result = Collections.synchronizedSet(new HashSet())
            'abc'.eachParallel {result.add(it.toUpperCase())}
            assertEquals(new HashSet(['A', 'B', 'C']), result)
        }
    }

    public void testCollectParallelWithThreadPoolAndString() {
        Parallelizer.withParallelizer(5) {
            def result = 'abc'.collectParallel {it.toUpperCase()}
            assertEquals(['A', 'B', 'C'], result)
        }
    }

    public void testFindAllParallelWithThreadPoolAndString() {
        Parallelizer.withParallelizer(5) {
            def result = 'aBC'.findAllParallel {it == it.toUpperCase()}
            assertEquals(['B', 'C'], result)
        }
    }

    public void testFindParallelWithThreadPoolAndString() {
        Parallelizer.withParallelizer(5) {
            def result = 'aBC'.findParallel {it == it.toUpperCase()}
            assert (result in ['B', 'C'])
        }
    }

    public void testGrepParallelWithThreadPoolAndString() {
        Parallelizer.withParallelizer(5) {
            def result = 'aBC'.grepParallel('B')
            assertEquals (['B'], result)
        }
    }

    public void testAnyParallelWithThreadPoolAndString() {
        Parallelizer.withParallelizer(5) {
            assert 'aBc'.anyParallel {it == it.toUpperCase()}
            assert !'abc'.anyParallel {it == it.toUpperCase()}
        }
    }

    public void testAllParallelWithThreadPoolAndString() {
        Parallelizer.withParallelizer(5) {
            assert !'aBC'.everyParallel {it == it.toUpperCase()}
            assert 'ABC'.everyParallel() {it == it.toUpperCase()}
        }
    }
}
