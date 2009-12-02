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
class MakeTransparentTest extends GroovyTestCase {

    public void testMakeTransparentAvailability() {
        shouldFail {
            [].makeTransparent()
            [1].makeTransparent()
            'abcde'.makeTransparent()
        }

        def items = [1, 2, 3, 4, 5]

        shouldFail {
            items.makeTransparent()
        }

        Parallelizer.doParallel {
            assertNotNull([1].makeTransparent())
            assertNotNull('abcde'.makeTransparent())
            assertTrue items == items.makeTransparent()
            assertNotNull(items.makeTransparent())
            assertTrue items.makeTransparent() == items.makeTransparent().makeTransparent()
            final def p1 = items.makeTransparent()
            assertTrue p1 == p1.makeTransparent()
        }

        shouldFail {
            [1].makeTransparent()
        }

        shouldFail {
            items.makeTransparent()
        }
    }

    public void testMakeTransparentTypeCompatibility() {
        Parallelizer.doParallel {
            Collection c = [1, 2, 3, 4, 5].makeTransparent()
            String s = 'abcde'.makeTransparent()
        }
    }

    public void testIsTransparentCheck() {
        def items = [1, 2, 3, 4, 5]
        shouldFail {
            items.isTransparent()
        }
        shouldFail(IllegalStateException) {
            ParallelArrayUtil.makeTransparent(items)
        }
        Parallelizer.doParallel {
            assertFalse items.isTransparent()
            assertFalse 'abc'.isTransparent()
            assertTrue items.makeTransparent().isTransparent()
            assertTrue items.isTransparent()
            assertTrue 'abcde'.makeTransparent().isTransparent()
        }

        assertTrue items.isTransparent()
        assertTrue 'abcde'.isTransparent()
        shouldFail {
            assertTrue 'ab'.isTransparent()
        }

        shouldFail(IllegalStateException) {
            ParallelArrayUtil.makeTransparent('abcdefgh2')
        }
    }

    public void testIdempotenceOfNestingMakeTransparent() {
        def items = [1, 2, 3, 4, 5]
        final ConcurrentHashMap map = new ConcurrentHashMap()
        Parallelizer.doParallel(5) {
            items.makeTransparent().makeTransparent().each {
                Thread.sleep 500
                map[Thread.currentThread()] = ''
            }
        }
        assert map.keys().size() > 1
    }

    public void testMakeTransparentPropagationToResults() {
        def items = [1, 2, 3, 4, 5]
        final ConcurrentHashMap map = new ConcurrentHashMap()
        Parallelizer.doParallel(5) {
            items.makeTransparent().collect {it * 2}.findAll {it > 1}.each {
                Thread.sleep 500
                map[Thread.currentThread()] = ''
            }
        }
        assert map.keys().size() > 1
    }

    public void testNoMakeTransparentPropagationToResultsWithGroupBy() {
        def items = [1, 2, 3, 4, 5]
        final ConcurrentHashMap map = new ConcurrentHashMap()
        Parallelizer.doParallel(5) {
            items.makeTransparent().groupBy {it % 2}.each {
                Thread.sleep 500
                map[Thread.currentThread()] = ''
            }
        }
        assert map.keys().size() == 1
    }

    public void testMakeTransparentPropagationToResultsWithString() {
        def items = 'abcde'
        final ConcurrentHashMap map = new ConcurrentHashMap()
        Parallelizer.doParallel(5) {
            items.makeTransparent().collect {it * 2}.findAll {it.size() > 1}.each {
                Thread.sleep 500
                map[Thread.currentThread()] = ''
            }
        }
        assert map.keys().size() > 1
    }

    public void testMakeTransparentPropagationToResultsWithIterator() {
        def items = [1, 2, 3, 4, 5].iterator()
        final ConcurrentHashMap map = new ConcurrentHashMap()
        Parallelizer.doParallel(5) {
            items.makeTransparent().collect {it * 2}.findAll {it > 1}.each {
                Thread.sleep 500
                map[Thread.currentThread()] = ''
            }
        }
        assert map.keys().size() > 1
    }

    public void testTransparentParallelInMethodCall() {
        def items = [1, 2, 3, 4, 5]
        assertEquals 1, foo(items).keys().size()

        Parallelizer.doParallel(5) {
            assertEquals 1, foo(items).keys().size()
            assert foo(items.makeTransparent()).keys().size() > 3
        }
    }

    private def foo(Collection c) {
        final ConcurrentHashMap map = new ConcurrentHashMap()
        c.collect {it * 2}.findAll {it > 1}.each {
            Thread.sleep 50
            map[Thread.currentThread()] = ''
        }
        return map
    }
}
