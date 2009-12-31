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

class MakeTransparentCornerCaseTest extends GroovyTestCase {

    public void testUsingNonTransparentEachInTransparentContext() {
        def items = [1, 2, 3, 4, 5]
        final ConcurrentHashMap map = new ConcurrentHashMap()
        Parallelizer.doParallel(5) {
            items.makeTransparent().eachParallel {
                Thread.sleep 100
                map[Thread.currentThread()] = ''
            }
        }
        assert map.keys().size() > 1
    }

    public void testUsingNonTransparentCollectInTransparentContextWithString() {
        def items = 'abcdefg1'
        final ConcurrentHashMap map = new ConcurrentHashMap()
        Parallelizer.doParallel(5) {
            def result = items.makeTransparent().collectParallel {
                Thread.sleep 100
                map[Thread.currentThread()] = ''
                return false
            }
            assertFalse result.isTransparent()
        }
        assert map.keys().size() > 1
    }

    public void testUsingNonTransparentFindAllInTransparentContextWithString() {
        def items = 'abcdefg1'
        final ConcurrentHashMap map = new ConcurrentHashMap()
        Parallelizer.doParallel(5) {
            def result = items.makeTransparent().findAllParallel {
                Thread.sleep 100
                map[Thread.currentThread()] = ''
                return false
            }
            assertFalse result.isTransparent()
        }
        assert map.keys().size() > 1
    }

    public void testUsingNonTransparentGroupByInTransparentContextWithString() {
        def items = 'abcdefg1'
        final ConcurrentHashMap map = new ConcurrentHashMap()
        Parallelizer.doParallel(5) {
            items.makeTransparent().groupByParallel {
                Thread.sleep 100
                map[Thread.currentThread()] = ''
                return 'a'
            }
        }
        assert map.keys().size() > 1
    }
}