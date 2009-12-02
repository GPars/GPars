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

import java.util.concurrent.ConcurrentHashMap

/**
 * Author: Vaclav Pech
 * Date: Oct 30, 2009
 */

class MakeTransparentMethodEnhancerTest extends GroovyTestCase {

    public void testTransparentEach() {
        def items = [1, 2, 3, 4, 5]
        final ConcurrentHashMap map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeTransparent().each {
            Thread.sleep 100
            map[Thread.currentThread()] = ''
        }
        assert map.keys().size() > 1
    }

    public void testTransparentEachWithIndex() {
        def items = [1, 2, 3, 4, 5]
        final ConcurrentHashMap map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeTransparent().eachWithIndex {e, i ->
            Thread.sleep 100
            map[Thread.currentThread()] = ''
        }
        assert map.keys().size() > 1
    }

    public void testTransparentCollect() {
        def items = [1, 2, 3, 4, 5]
        final ConcurrentHashMap map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeTransparent().collect {
            Thread.sleep 100
            map[Thread.currentThread()] = ''
            return it
        }
        assert map.keys().size() > 1
    }

    public void testTransparentGrep() {
        def items = [1, 2, 3, 4, 5]
        final ConcurrentHashMap map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeTransparent().grep {
            Thread.sleep 100
            map[Thread.currentThread()] = ''
            return true
        }
        assert map.keys().size() > 1
    }

    public void testTransparentFind() {
        def items = [1, 2, 3, 4, 5]
        final ConcurrentHashMap map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeTransparent().find {
            Thread.sleep 100
            map[Thread.currentThread()] = ''
            return false
        }
        assert map.keys().size() > 1
    }

    public void testTransparentFindAll() {
        def items = [1, 2, 3, 4, 5]
        final ConcurrentHashMap map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeTransparent().findAll {
            Thread.sleep 100
            map[Thread.currentThread()] = ''
            return true
        }
        assert map.keys().size() > 1
    }

    public void testTransparentAll() {
        def items = [1, 2, 3, 4, 5]
        final ConcurrentHashMap map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeTransparent().every {
            Thread.sleep 100
            map[Thread.currentThread()] = ''
            return false
        }
        assert map.keys().size() > 1
    }

    public void testTransparentAny() {
        def items = [1, 2, 3, 4, 5]
        final ConcurrentHashMap map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeTransparent().any {
            Thread.sleep 100
            map[Thread.currentThread()] = ''
            return false
        }
        assert map.keys().size() > 1
    }

    public void testTransparentAnyOnString() {
        def items = 'abcdefg'
        final ConcurrentHashMap map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeTransparent().any {
            Thread.sleep 100
            map[Thread.currentThread()] = ''
            return false
        }
        assert map.keys().size() > 1
    }

    public void testTransparentGroupBy() {
        def items = [1, 2, 3, 4, 5]
        final ConcurrentHashMap map = new ConcurrentHashMap()
        ParallelEnhancer.enhanceInstance(items)
        items.makeTransparent().groupBy {
            Thread.sleep 100
            map[Thread.currentThread()] = ''
            return it
        }
        assert map.keys().size() > 1
    }
}