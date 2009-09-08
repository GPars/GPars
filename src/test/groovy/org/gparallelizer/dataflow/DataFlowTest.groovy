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

package org.gparallelizer.dataflow

import static org.gparallelizer.dataflow.DataFlow.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

public class DataFlowTest extends GroovyTestCase {

    public void testSimpleAssignment() {
        DataFlowVariable<Integer> x = new DataFlowVariable()
        DataFlowVariable<Integer> y = new DataFlowVariable()
        DataFlowVariable<Integer> z = new DataFlowVariable()

        volatile def result = 0
        final def latch = new CountDownLatch(1)

        start {
            z << x.val + y.val
            result = z.val
            latch.countDown()
        }

        start {
          x << 40
        }
        start {
          y << 2
        }

        latch.await(30, TimeUnit.SECONDS)
        assertEquals 42, result
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
        def x = new DataFlowVariable<List<Integer>>()
        def y = new DataFlowVariable<List<Integer>>()

        volatile def result = 0
        final def latch = new CountDownLatch(1)

        start { x << ints(0, 10) }
        start { y << sum(0, x.val) }
        start {
            result = y.val
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)
        assertEquals([0, 0, 1, 3, 6, 10, 15, 21, 28, 36, 45], result)
    }

    void testRightShift() {
        DataFlowVariable<Integer> x = new DataFlowVariable()
        DataFlowVariable<Integer> y = new DataFlowVariable()
        DataFlowVariable<Integer> z = new DataFlowVariable()

        volatile def result = 0
        final def latch = new CountDownLatch(1)

        z >> { res ->
          result = res
          latch.countDown()
        }

        start {
            z << x.val + y.val
        }

        start {x << 40}
        start {y << 2}

        latch.await(30, TimeUnit.SECONDS)
        assertEquals 42, result
    }

    void testMethodSyntax () {
      def df = new DataFlows ()

      volatile def result = 0
      final def latch = new CountDownLatch(1)

      df.z { res ->
        result = res
        latch.countDown()
      }

      start {
          def v = df.x + df.y
          df.z = v
      }

      start {
        df.x = 40
      }
      start {
        df.y = 2
      }

      latch.await(30, TimeUnit.SECONDS)
      assertEquals 42, result
    }
}
