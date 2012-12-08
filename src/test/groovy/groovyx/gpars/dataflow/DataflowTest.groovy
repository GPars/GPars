// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2012  The original author or authors
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

package groovyx.gpars.dataflow

import groovyx.gpars.group.NonDaemonPGroup

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import static groovyx.gpars.actor.Actors.blockingActor

public class DataflowTest extends GroovyTestCase {

    public void testSimpleAssignment() {
        DataflowVariable<Integer> x = new DataflowVariable()
        DataflowVariable<Integer> y = new DataflowVariable()
        DataflowVariable<Integer> z = new DataflowVariable()

        def result = 0
        final def latch = new CountDownLatch(1)

        blockingActor {
            z << x.val + y.val
            result = z.val
            latch.countDown()
        }

        blockingActor {
            x << 40
        }
        blockingActor {
            y << 2
        }

        latch.await(90, TimeUnit.SECONDS)
        assert 42 == result
    }

    List<Integer> ints(int n, int max) {
        if (n == max) return []
        else return [n, * ints(n + 1, max)]
    }

    List<Integer> sum(int s, List<Integer> stream) {
        switch (stream.size()) {
            case 0: return [s]
            default:
                return [s, * sum(stream[0] + s, stream.size() > 1 ? stream[1..-1] : [])]
        }
    }

    public void testListAssignment() {
        def x = new DataflowVariable<List<Integer>>()
        def y = new DataflowVariable<List<Integer>>()

        blockingActor { x << ints(0, 10) }
        blockingActor { y << sum(0, x.val) }

        assert [0, 0, 1, 3, 6, 10, 15, 21, 28, 36, 45] == y.val
    }

    void testRightShift() {
        DataflowVariable<Integer> x = new DataflowVariable()
        DataflowVariable<Integer> y = new DataflowVariable()
        DataflowVariable<Integer> z = new DataflowVariable()

        def result = new DataflowVariable()

        z >> { res ->
            result << res
        }

        blockingActor {
            z << x.val + y.val
        }

        blockingActor { x << 40 }
        blockingActor { y << 2 }

        assert 42 == result.val
    }

    void testMethodSyntax() {
        def df = new Dataflows()

        def result = new DataflowVariable()

        df.z { res ->
            result << res
        }

        blockingActor {
            def v = df.x + df.y
            df.z = v
        }

        blockingActor {
            df.x = 40
        }
        blockingActor {
            df.y = 2
        }

        assert 42 == result.val
    }

    void testWhenAllBound() {
        final promises = (1..5).collect { new DataflowVariable() }
        final result = Dataflow.whenAllBound(promises) { a, b, c, d, e -> a + b + c + d + e }
        Thread.start {
            promises.eachWithIndex { p, i -> sleep 100; p << i + 1 }
        }
        assert result.val == 15
    }

    public void testNestedWhenAllBoundChaining() {
        final DataflowVariable variable = new DataflowVariable()

        def group = new NonDaemonPGroup()
        Thread.start {
            sleep 500
            variable << 4
        }
        assert 8 == group.whenAllBound([variable]) { value ->
            def a = new DataflowVariable()
            Thread.start {
                sleep 500
                a << value * 2
            }
            return a
        }.val
        group.shutdown()
    }

    void testWhenAllBoundWithListArgument() {
        final promises = (1..5).collect { new DataflowVariable() }
        final result = Dataflow.whenAllBound(promises) { List values -> values.sum() }
        Thread.start {
            promises.eachWithIndex { p, i -> sleep 100; p << i + 1 }
        }
        assert result.val == 15
    }

    void testWhenAllBoundWithListArgumentAndOneValue() {
        final promises = (1..1).collect { new DataflowVariable() }
        final result = Dataflow.whenAllBound(promises) { List values -> values.sum() }
        Thread.start {
            promises.eachWithIndex { p, i -> sleep 100; p << i + 1 }
        }
        assert result.val == 1
    }

    void testWhenAllBoundWithListArgumentAndOneListValue() {
        final promises = (1..1).collect { new DataflowVariable() }
        final result = Dataflow.whenAllBound(promises) { value -> value.reverse() }
        Thread.start {
            promises.eachWithIndex { p, i -> sleep 100; p << [i + 1, 0] }
        }
        assert result.val == [0, 1]
    }

    void testWhenAllBoundWithListArgumentAndOneListValueWithSpecifiedType() {
        final promises = (1..1).collect { new DataflowVariable() }
        final result = Dataflow.whenAllBound(promises) { List value -> value.reverse() }
        Thread.start {
            promises.eachWithIndex { p, i -> sleep 100; p << [i + 1, 0] }
        }
        assert result.val == [[1, 0]]
    }

    void testWhenAllBoundWithMultiplePromisesAndListArguments() {
        final promise1 = new DataflowVariable()
        final promise2 = new DataflowVariable()
        final promise3 = new DataflowVariable()
        final result = Dataflow.whenAllBound(promise1, promise2, promise3) { List value -> value.reverse() }
        Thread.start {
            promise1 << 1
            promise2 << 2
            promise3 << 3
        }
        assert result.val == [3, 2, 1]
    }

    void testWhenAllBoundWithMultiplePromisesAndExplicitArguments() {
        final promise1 = new DataflowVariable()
        final promise2 = new DataflowVariable()
        final promise3 = new DataflowVariable()
        final result = Dataflow.whenAllBound(promise1, promise2, promise3) { a, b, c -> [c, b, a] }
        Thread.start {
            promise1 << 1
            promise2 << 2
            promise3 << 3
        }
        assert result.val == [3, 2, 1]
    }
}
