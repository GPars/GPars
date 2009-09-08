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

package org.gparallelizer.samples

import java.util.concurrent.CountDownLatch
import org.gparallelizer.actors.pooledActors.PooledActors
import static org.gparallelizer.actors.pooledActors.PooledActors.*
import org.gparallelizer.actors.pooledActors.PooledActorGroup
import org.gparallelizer.actors.ThreadActorGroup

/**
 * Performs merge sort using pooled actors.
 * @author Vaclav Pech
 */
protected def split(List<Integer> list) {
    int listSize = list.size()
    int middleIndex = listSize / 2
    def list1 = list[0..<middleIndex]
    def list2 = list[middleIndex..listSize - 1]
    return [list1, list2]
}

protected List<Integer> merge(List<Integer> a, List<Integer> b) {
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
        disableSendingReplies()
        react {List<Integer> message ->
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

                    def child1 = actor(createMessageHandler(delegate))
                    def child2 = actor(createMessageHandler(delegate))
                    child1.start().send(splitList[0])
                    child2.start().send(splitList[1])

                    react {message1 ->
                        react {message2 ->
                            parentActor.send merge(message1, message2)
                        }

                    }
            }
        }
    }
}

final PooledActorGroup pooledGroup = new PooledActorGroup(3, true)
final ThreadActorGroup threadGroup = new ThreadActorGroup(3, false)

final List items = []
final Random random = new Random()
for(i in 1..100) {
    items << random.nextInt(10000)
}

def resultActor = threadGroup.actor {
    disableSendingReplies()
    final long t1 = System.currentTimeMillis()
    for(i in 1..10) {
        def sorter = pooledGroup.actor(createMessageHandler(delegate))
        sorter.start().send(items)
        receive()
    }
    final long t2 = System.currentTimeMillis()
    println "Event mergeort ${t2 - t1}"

    System.exit 0

}.start()


System.in.read()
