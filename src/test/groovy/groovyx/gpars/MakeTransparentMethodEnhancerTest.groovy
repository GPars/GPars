// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2013  The original author or authors
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
 * Author: Vaclav Pech
 * Date: Oct 30, 2009
 */

@SuppressWarnings("SpellCheckingInspection")
class MakeTransparentMethodEnhancerTest extends groovy.test.GroovyTestCase {

    public void testTransparentEach() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeConcurrent().each {
            Thread.sleep 100
            map[Thread.currentThread()] = ''
        }
        assert map.keys().size() > 1
    }

    public void testTransparentEachWithIndex() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeConcurrent().eachWithIndex { e, i ->
            Thread.sleep 100
            map[Thread.currentThread()] = ''
        }
        assert map.keys().size() > 1
    }

    public void testTransparentCollect() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeConcurrent().collect {
            Thread.sleep 100
            map[Thread.currentThread()] = ''
            return it
        }
        assert map.keys().size() > 1
    }

    public void testTransparentGrep() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeConcurrent().grep {
            Thread.sleep 100
            map[Thread.currentThread()] = ''
            return true
        }
        assert map.keys().size() > 1
    }

    public void testTransparentFind() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeConcurrent().find {
            Thread.sleep 100
            map[Thread.currentThread()] = ''
            return false
        }
        assert map.keys().size() > 1
    }

    public void testTransparentFindAny() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeConcurrent().findAny {
            Thread.sleep 100
            map[Thread.currentThread()] = ''
            return false
        }
        assert map.keys().size() > 1
    }

    public void testTransparentFindAll() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeConcurrent().findAll {
            Thread.sleep 100
            map[Thread.currentThread()] = ''
            return true
        }
        assert map.keys().size() > 1
    }

    public void testTransparentAll() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeConcurrent().every {
            Thread.sleep 100
            map[Thread.currentThread()] = ''
            return true
        }
        assert map.keys().size() > 1
    }

    public void testTransparentAny() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeConcurrent().any {
            Thread.sleep 100
            map[Thread.currentThread()] = ''
            return false
        }
        assert map.keys().size() > 1
    }

    public void testTransparentAnyOnString() {
        def items = 'abcdefg'
        final Map map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeConcurrent().any {
            Thread.sleep 100
            map[Thread.currentThread()] = ''
            return false
        }
        assert map.keys().size() > 1
    }

    public void testTransparentGroupBy() {
        def items = [1, 2, 3, 4, 5]
        final Map map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeConcurrent().groupBy {
            Thread.sleep 100
            map[Thread.currentThread()] = ''
            return it
        }
        assert map.keys().size() > 1
    }
}
