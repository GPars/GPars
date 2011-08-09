// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
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
@SuppressWarnings("GroovyEmptyCatchBlock") class ConditionalLoopTest extends GroovyTestCase {
    public void testNoLoop() {
        int result = 0
        def actor = actor {
            loop({-> false}) {
                result = 1
            }
//            result = 2
        }
        actor.join()
        assert result == 0
    }

    public void testNoLoopWithTail() {
        int result = 0
        def actor = actor {
            loop({-> false}) {
                result = 1
            }
            result = 2
        }
        actor.join()
        assert result == 2
    }

    public void testNoLoopOnNumberOfIterations() {
        int result = 0
        def actor = actor {
            loop(0) {
                result = 1
            }
//            result = 2
        }
        actor.join()
        assert result == 0
    }

    public void testSingleLoop() {
        int result = 0
        def actor = actor {
            Integer counter = 0

            loop({-> counter < 1}) {
                counter++
                result++
            }
//            result = 100
        }
        actor.join()
        assert result == 1
    }

    public void testSingleLoopOnNumberOfIterations() {
        int result = 0
        def actor = actor {
            loop(1) {
                result++
            }
//            result = 100
        }
        actor.join()
        assert result == 1
    }

    public void testRepeatedLoop() {
        int result = 0
        def actor = actor {
            Integer counter = 0

            loop({-> counter < 5}) {
                counter++
                result++
            }
//            result = 100
        }
        actor.join()
        assert result == 5
    }

    public void testRepeatedLoopOnNumberOfIterations() {
        int result = 0
        def actor = actor {
            loop(5) {
                result++
            }
//            result = 100
        }
        actor.join()
        assert result == 5
    }

    public void testRepeatedLoopWithStop() {
        int result = 0
        def actor = actor {
            Integer counter = 0

            loop({-> counter < 5}) {
                counter++
                result++
                stop()
            }
//            result = 100
        }
        actor.join()
        assert result == 5
    }

    public void testRepeatedLoopOnNumberOfIterationsWithStop() {
        int result = 0
        def actor = actor {
            loop(5) {
                result++
                stop()
            }
//            result = 100
        }
        actor.join()
        assert result == 5
    }

    public void testRepeatedLoopWithTerminate() {
        int result = 0
        def actor = actor {
            Integer counter = 0

            loop({-> counter < 5}) {
                counter++
                result++
                terminate()
            }
//            result = 100
        }
        actor.join()
        assert result == 1
    }

    public void testRepeatedLoopOnNumberOfIterationsWithTerminate() {
        int result = 0
        def actor = actor {
            loop(5) {
                result++
                terminate()
            }
//            result = 100
        }
        actor.join()
        assert result == 1
    }

    public void testRepeatedLoopWithReact() {
        int result = 0
        def actor = actor {
            Integer counter = 0

            loop({-> counter < 5}) {
                counter++
                result++
                react {}
            }
//            result = 100
        }
        actor 1
        actor 2
        actor 3
        actor 4
        actor 5
        try {actor 6} catch (ignore) {}
        actor.join()
        assert result == 5
    }

    public void testRepeatedLoopOnNumberOfIterationsWithReact() {
        int result = 0
        def actor = actor {
            loop(5) {
                result++
                react {}
            }
//            result = 100
        }
        actor 1
        actor 2
        actor 3
        actor 4
        actor 5
        try {actor 6} catch (ignore) {}
        actor.join()
        assert result == 5
    }

    public void testNoLoopWithAfterLoopCode() {
        int result = 0
        def actor = actor {
            loop({-> false}, {-> result += 3}) {
                result = 1
            }
//            result = 2
        }
        actor.join()
        assert result == 3
    }

    public void testSingleLoopWithAfterLoopCode() {
        int result = 0
        def actor = actor {
            Integer counter = 0

            loop({-> counter < 1}, {-> result += 3}) {
                counter++
                result++
            }
//            result = 100
        }
        actor.join()
        assert result == 4
    }

    public void testRepeatedLoopWithAfterLoopCode() {
        int result = 0
        def actor = actor {
            Integer counter = 0

            loop({-> counter < 5}, {-> result += 3}) {
                counter++
                result++
            }
//            result = 100
        }
        actor.join()
        assert result == 8
    }

    public void testComplexAfterLoopCode() {
        int result = 0
        def actor = actor {
            Integer counter = 0

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
//            result = 100
        }
        actor 1
        actor 2
        actor 3
        actor 4
        actor 5
        try {
            actor 6
            actor 7
            actor 8
        } catch (ignore) {}
        actor.join()
        assert result == 18
    }
}
