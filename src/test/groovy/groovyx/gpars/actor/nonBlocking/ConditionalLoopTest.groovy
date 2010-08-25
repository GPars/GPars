// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
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

package groovyx.gpars.actor.nonBlocking

import java.util.concurrent.Callable
import static groovyx.gpars.actor.Actors.actor

/**
 * @author Vaclav Pech
 * Date: Aug 25th 2010
 */
class ConditionalLoopTest extends GroovyTestCase {
    public void testNoLoop() {
        volatile int result = 0
        def actor = actor {
            loop({-> false} as Callable) {
                result = 1
            }
            result = 2
        }
        actor.join()
        assert result == 0
    }

    public void testNoLoopOnNumberOfIterations() {
        volatile int result = 0
        def actor = actor {
            loop(0) {
                result = 1
            }
            result = 2
        }
        actor.join()
        assert result == 0
    }

    public void testSingleLoop() {
        volatile int result = 0
        def actor = actor {
            int counter = 0

            loop({-> counter < 1} as Callable) {
                counter++
                result++
            }
            result = 100
        }
        actor.join()
        assert result == 1
    }

    public void testSingleLoopOnNumberOfIterations() {
        volatile int result = 0
        def actor = actor {
            loop(1) {
                result++
            }
            result = 100
        }
        actor.join()
        assert result == 1
    }

    public void testRepeatedLoop() {
        volatile int result = 0
        def actor = actor {
            int counter = 0

            loop({-> counter < 5} as Callable) {
                counter++
                result++
            }
            result = 100
        }
        actor.join()
        assert result == 5
    }

    public void testRepeatedLoopOnNumberOfIterations() {
        volatile int result = 0
        def actor = actor {
            loop(5) {
                result++
            }
            result = 100
        }
        actor.join()
        assert result == 5
    }

    public void testRepeatedLoopWithStop() {
        volatile int result = 0
        def actor = actor {
            int counter = 0

            loop({-> counter < 5} as Callable) {
                counter++
                result++
                stop()
            }
            result = 100
        }
        actor.join()
        assert result == 1
    }

    public void testRepeatedLoopOnNumberOfIterationsWithStop() {
        volatile int result = 0
        def actor = actor {
            loop(5) {
                result++
                stop()
            }
            result = 100
        }
        actor.join()
        assert result == 1
    }

    public void testRepeatedLoopWithTerminate() {
        volatile int result = 0
        def actor = actor {
            int counter = 0

            loop({-> counter < 5} as Callable) {
                counter++
                result++
                terminate()
            }
            result = 100
        }
        actor.join()
        assert result == 1
    }

    public void testRepeatedLoopOnNumberOfIterationsWithTerminate() {
        volatile int result = 0
        def actor = actor {
            loop(5) {
                result++
                terminate()
            }
            result = 100
        }
        actor.join()
        assert result == 1
    }
}
