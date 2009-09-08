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
public class ParallelizerTest extends GroovyTestCase {
    public void testEachAsyncWithThreadPool() {
        Parallelizer.doParallel(5) {
          def result = Collections.synchronizedSet(new HashSet())
            [1, 2, 3, 4, 5].eachAsync {Number number -> result.add(number * 10)}
            assertEquals(new HashSet([10, 20, 30, 40, 50]), result)
        }
    }

    public void testEachAsyncOnOneElementCollections() {
        Parallelizer.doParallel(5) {
            [1].eachAsync{}
            [1].eachAsync{}
            [1].eachAsync{}
            'a'.eachAsync{}
            [1].iterator().eachAsync{}
            'a'.iterator().eachAsync{}
        }
    }

    public void testEachAsyncOnEmpty() {
        Parallelizer.doParallel(5) {
            [].eachAsync{throw new RuntimeException('Should not be thrown')}
            [].eachAsync{throw new RuntimeException('Should not be thrown')}
            [].eachAsync{throw new RuntimeException('Should not be thrown')}
            ''.eachAsync{throw new RuntimeException('Should not be thrown')}
            [].iterator().eachAsync{throw new RuntimeException('Should not be thrown')}
            ''.iterator().eachAsync{throw new RuntimeException('Should not be thrown')}
        }
    }

    public void testCollectAsyncWithThreadPool() {
        Parallelizer.doParallel(5) {
            def result = [1, 2, 3, 4, 5].collectAsync {Number number -> number * 10}
            assertEquals([10, 20, 30, 40, 50], result)
        }
    }

    public void testFindAllAsyncWithThreadPool() {
        Parallelizer.doParallel(5) {
            def result = [1, 2, 3, 4, 5].findAllAsync {Number number -> number > 3}
            assertEquals([4, 5], result)
        }
    }

    public void testFindAsyncWithThreadPool() {
        Parallelizer.doParallel(5) {
            def result = [1, 2, 3, 4, 5].findAsync {Number number -> number > 3}
            assert (result in [4, 5])
        }
    }

    public void testAnyAsyncWithThreadPool() {
        Parallelizer.doParallel(5) {
            assert [1, 2, 3, 4, 5].anyAsync {Number number -> number > 3}
            assert ![1, 2, 3].anyAsync {Number number -> number > 3}
        }
    }

    public void testAllAsyncWithThreadPool() {
        Parallelizer.doParallel(5) {
            assert ![1, 2, 3, 4, 5].allAsync {Number number -> number > 3}
            assert [1, 2, 3].allAsync() {Number number -> number <= 3}
        }
    }

    public void testCategoryUsage() {
        Parallelizer.doParallel(5) {
            assertEquals(new HashSet([2, 4, 6]), new HashSet((Collection)new HashSet([1, 2, 3]).collectAsync {it * 2}))
            assertEquals(new HashSet([2, 3]), new HashSet((Collection)[1, 2, 3].findAllAsync {it > 1}))
        }
    }
}
