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

package groovyx.gpars.actor.blocking

import groovyx.gpars.actor.Actors
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.scheduler.ResizeablePool
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 *
 * @author Vaclav Pech
 * Date: Jan 12, 2009
 */
abstract public class MergeSortTest extends GroovyTestCase {

    def group

    protected void setUp() {
        super.setUp();    //To change body of overridden methods use File | Settings | File Templates.
        group = new DefaultPGroup(new ResizeablePool(false, 50))
    }


    protected void tearDown() {
        group.shutdown()
        super.tearDown()
    }


    private def split(List<Integer> list) {
        int listSize = list.size()
        int middleIndex = listSize / 2
        def list1 = list[0..<middleIndex]
        def list2 = list[middleIndex..listSize - 1]
        return [list1, list2]
    }

    private List<Integer> merge(List<Integer> a, List<Integer> b) {
        int i = 0, j = 0
        final int newSize = a.size() + b.size()
        List<Integer> result = new ArrayList<Integer>(newSize)

        while ((i < a.size()) && (j < b.size())) {
            if (a[i] <= b[j]) result << a[i++]
            else result << b[j++]
        }

        if (i < a.size()) result.addAll(a[i..-1])
        else result.addAll(b[j..-1])
        return result
    }

    Closure createMessageHandler(def parentActor) {
        return {
            receive {List<Integer> message ->
                assert message != null
                switch (message.size()) {
                    case 0..1:
                        parentActor.send(message)
                        break
                    case 2:
                        if (message[0] <= message[1]) parentActor.send(message)
                        else parentActor.send(message[-1..0])

                        break
                    default:
                        def splitList = split(message)

                        def child1 = group.blockingActor(createMessageHandler(delegate))
                        def child2 = group.blockingActor(createMessageHandler(delegate))
                        child1.send(splitList[0])
                        child2.send(splitList[1])

                        parentActor.send merge(receive(), receive())
                }
            }
        }

    }

    public void testDefaultMergeSort() {
        def result = null;
        final CountDownLatch latch = new CountDownLatch(1)

        def resultActor = Actors.blockingActor {
            result = receive(90, TimeUnit.SECONDS)
            latch.countDown()
        }

        def sorter = Actors.blockingActor(createMessageHandler(resultActor))
        sorter.send([1, 5, 2, 4, 3, 8, 6, 7, 3, 9, 5, 3])

        latch.await(90, TimeUnit.SECONDS)
        assert [1, 2, 3, 3, 3, 4, 5, 5, 6, 7, 8, 9] == result
    }
}
