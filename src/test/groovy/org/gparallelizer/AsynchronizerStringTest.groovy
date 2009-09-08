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

import java.util.concurrent.ExecutorService

/**
 * @author Vaclav Pech
 * Date: Nov 10, 2008
 */
public class AsynchronizerStringTest extends GroovyTestCase {

    public void testEachAsyncWithString() {
      def result = Collections.synchronizedSet(new HashSet())
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            'abc'.eachAsync {result.add(it.toUpperCase())}
            assertEquals(new HashSet(['A', 'B', 'C']), result)
        }
    }

    public void testCollectAsyncWithString() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            def result = 'abc'.collectAsync{it.toUpperCase()}
            assertEquals(['A', 'B', 'C'], result)
        }
    }

    public void testFindAllAsyncWithString() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            def result = 'aBC'.findAllAsync{it == it.toUpperCase()}
            assertEquals(['B', 'C'], result)
        }
    }

    public void testFindAsyncWithString() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            def result = 'aBC'.findAsync{it == it.toUpperCase()}
            assert result in ['B', 'C']
        }
    }

    public void testAllAsyncWithString() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            assert 'ABC'.allAsync{it == it.toUpperCase()}
            assert !'aBC'.allAsync{it == it.toUpperCase()}
        }
    }

    public void testAnyAsyncWithString() {
        Asynchronizer.withAsynchronizer(5) {ExecutorService service ->
            assert 'aBc'.anyAsync{it == it.toUpperCase()}
            assert 'aBC'.anyAsync{it == it.toUpperCase()}
            assert !'abc'.anyAsync{it == it.toUpperCase()}
        }
    }
}
