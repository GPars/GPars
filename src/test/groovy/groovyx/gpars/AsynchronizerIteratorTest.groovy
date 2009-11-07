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

public class AsynchronizerIteratorTest extends GroovyTestCase {
    public void testIteratorEach() {
        def list = [1, 2, 3, 4, 5, 6, 7, 8, 9]

        Asynchronizer.withAsynchronizer {
            def result = Collections.synchronizedSet(new HashSet())
            list.iterator().eachParallel {
                result << it
            }
            assertEquals 9, result.size()
        }
    }

    public void testIteratorCollect() {
        def list = [1, 2, 3, 4, 5, 6, 7, 8, 9]

        Asynchronizer.withAsynchronizer {
            def result = list.iterator().collectParallel { 2 * it }
            assertEquals 9, result.size()
            assert result.any {it == 12}
        }
    }

    public void testIterator() {
        def list = [1, 2, 3, 4, 5, 6, 7, 8, 9]

        Asynchronizer.withAsynchronizer {
            assert list.iterator().anyParallel { it == 6 }
            assert list.iterator().everyParallel { it < 10 }
            assertEquals 8, list.iterator().findParallel { it == 8 }
            assertEquals 3, (list.iterator().findAllParallel { it > 6 }).size()
        }
    }
}
