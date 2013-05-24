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

package groovyx.gpars.actor

import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.group.PGroup

class MemoryPropagationTest extends GroovyTestCase {
    private static final int ITERATIONS = 50000

    public void testMemoryPropagation() {

        final PGroup group = new DefaultPGroup(3)

        group.with {
            def a = new MemoryPropagationTestActor(parallelGroup: group).start()
            a.makeFair()  //Fair actors always return to pool after handling a single message
            ITERATIONS.times {
                a 1
            }
            def result = a.sendAndWait('print')
            assert ITERATIONS == result[0]
            assert ITERATIONS == result[1]
            assert 1 < result[2]
            a.stop()
        }


        group.shutdown()
    }

}
class MemoryPropagationTestActor extends DynamicDispatchActor {
    long counter1 = 0
    long counter2 = 0
    def threads = [] as Set

    void onMessage(String printRequest) {
        reply([counter1, counter2, threads.size()])
    }

    void onMessage(Integer value) {
        counter1++
        counter2++
        threads << Thread.currentThread()
    }
}
