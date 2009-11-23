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

package groovyx.gpars.samples.actors

import groovyx.gpars.actor.Actors
import groovyx.gpars.actor.PooledActorGroup
import groovyx.gpars.scheduler.ResizablePool

/**
 * Performs merge sort using pooled actors.
 * @author Vaclav Pech
 * Date: Jan 7, 2009
 */
def split(List<Integer> list) {
    int listSize = list.size()
    int middleIndex = listSize / 2
    def list1 = list[0..<middleIndex]
    def list2 = list[middleIndex..listSize - 1]
    return [list1, list2]
}

List<Integer> merge(List<Integer> a, List<Integer> b) {
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

class GroupTestHeper {
    public static final def group = new PooledActorGroup(new ResizablePool(true))
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

                    def child1 = GroupTestHeper.group.actor(createMessageHandler(delegate))
                    def child2 = GroupTestHeper.group.actor(createMessageHandler(delegate))
                    child1.send(splitList[0])
                    child2.send(splitList[1])

                    parentActor.send merge(receive(), receive())
            }
        }
    }

}

def resultActor = Actors.actor {
    println "Sorted array:\t${receive()}"
}

def sorter = Actors.actor(createMessageHandler(resultActor))
sorter.send([1, 5, 2, 4, 3, 8, 6, 7, 3,
        4, 5, 2, 2, 9, 8, 7, 6, 7, 8, 1, 4, 1, 7, 5, 8, 2, 3, 9, 5, 7, 4, 3])

resultActor.join()
