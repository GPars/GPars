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

import static groovyx.gpars.actor.Actors.actor

/**
 * @author Vaclav Pech
 * Date: Aug 25th 2010
 */
class ConditionalLoopTest extends GroovyTestCase {
    public void testNoLoop() {
        volatile int result = 0
        def actor = actor {
            loop({-> false}) {
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

            loop({-> counter < 1}) {
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

            loop({-> counter < 5}) {
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

            loop({-> counter < 5}) {
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

            loop({-> counter < 5}) {
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

    public void testRepeatedLoopWithReact() {
        volatile int result = 0
        def actor = actor {
            int counter = 0

            loop({-> counter < 5}) {
                counter++
                result++
                react {}
            }
            result = 100
        }
        actor 1
        actor 2
        actor 3
        actor 4
        actor 5
        actor 6
        actor.join()
        assert result == 5
    }

    public void testRepeatedLoopOnNumberOfIterationsWithReact() {
        volatile int result = 0
        def actor = actor {
            loop(5) {
                result++
                react {}
            }
            result = 100
        }
        actor 1
        actor 2
        actor 3
        actor 4
        actor 5
        actor 6
        actor.join()
        assert result == 5
    }

    public void testNoLoopWithAfterLoopCode() {
        volatile int result = 0
        def actor = actor {
            loop({-> false}, {-> result += 3}) {
                result = 1
            }
            result = 2
        }
        actor.join()
        assert result == 3
    }

    public void testSingleLoopWithAfterLoopCode() {
        volatile int result = 0
        def actor = actor {
            int counter = 0

            loop({-> counter < 1}, {-> result += 3}) {
                counter++
                result++
            }
            result = 100
        }
        actor.join()
        assert result == 4
    }

    public void testRepeatedLoopWithAfterLoopCode() {
        volatile int result = 0
        def actor = actor {
            int counter = 0

            loop({-> counter < 5}, {-> result += 3}) {
                counter++
                result++
            }
            result = 100
        }
        actor.join()
        assert result == 8
    }

    public void testComplexAfterLoopCode() {
        volatile int result = 0
        def actor = actor {
            int counter = 0

            def afterLoopCode = {->
                react {
                    result += it
                    react {
                        result += it
                    }
                }
            }
            loop({-> counter < 5}, afterLoopCode) {
                counter++
                result++
                react {}
            }
            result = 100
        }
        actor 1
        actor 2
        actor 3
        actor 4
        actor 5
        actor 6
        actor 7
        actor 8
        actor.join()
        assert result == 18
    }

    public void testLoopingAfterLoopCode() {
        volatile int result = 0
        volatile exception = null
        def actor = actor {
            int counter = 0

            def afterLoopCode = {->
                try {
                    loop { }
                } catch (all) {
                    exception = all
                }
            }
            loop({-> counter < 5}, afterLoopCode) {
                counter++
                result++
                react {}
            }
            result = 100
        }
        actor 1
        actor 2
        actor 3
        actor 4
        actor 5
        actor 6
        actor 7
        actor 8
        actor.join()
        assert result == 5
        assert exception in IllegalStateException
    }
}
