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

/**
 * @author Vaclav Pech
 * Date: Oct 23, 2008
 */
public class ParallelizerStringTest extends GroovyTestCase {
    public void testEachAsyncWithThreadPoolAndString() {
        Parallelizer.withParallelizer(5) {
          def result = Collections.synchronizedSet(new HashSet())
            'abc'.eachAsync {result.add(it.toUpperCase())}
            assertEquals(new HashSet(['A', 'B', 'C']), result)
        }
    }

    public void testCollectAsyncWithThreadPoolAndString() {
        Parallelizer.withParallelizer(5) {
            def result = 'abc'.collectAsync {it.toUpperCase()}
            assertEquals(['A', 'B', 'C'], result)
        }
    }

    public void testFindAllAsyncWithThreadPoolAndString() {
        Parallelizer.withParallelizer(5) {
            def result = 'aBC'.findAllAsync {it == it.toUpperCase()}
            assertEquals(['B', 'C'], result)
        }
    }

    public void testFindAsyncWithThreadPoolAndString() {
        Parallelizer.withParallelizer(5) {
            def result = 'aBC'.findAsync {it == it.toUpperCase()}
            assert (result in ['B', 'C'])
        }
    }

    public void testAnyAsyncWithThreadPoolAndString() {
        Parallelizer.withParallelizer(5) {
            assert 'aBc'.anyAsync {it == it.toUpperCase()}
            assert !'abc'.anyAsync {it == it.toUpperCase()}
        }
    }

    public void testAllAsyncWithThreadPoolAndString() {
        Parallelizer.withParallelizer(5) {
            assert !'aBC'.allAsync {it == it.toUpperCase()}
            assert 'ABC'.allAsync() {it == it.toUpperCase()}
        }
    }
}
