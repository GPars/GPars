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

package org.gparallelizer.actors

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 *
 * @author Vaclav Pech
 * Date: Jan 7, 2009
 */

public class ArraySumTest extends GroovyTestCase {

    public void testArraySummaryUsingActorMethod() {
        AtomicInteger result=new AtomicInteger(0)
        CountDownLatch latch=new CountDownLatch(1)

        Actor actor=Actors.actor {
            new Processor(delegate).start().send([1, 2, 3, 4, 5])
            receive {
                result.set it[0]
                latch.countDown()
            }
            stop()
        }
        actor.start()

        latch.await(30, TimeUnit.SECONDS)
        assertEquals 15, result
    }

    public void testArraySummary() {
        final ArrayCalculator calculator = new ArrayCalculator([1, 2, 3, 4, 5]).start()

        calculator.latch.await(30, TimeUnit.SECONDS)
        assertEquals 15, calculator.result
    }
}

class Processor extends DefaultThreadActor {

    Actor parent

    def Processor(Actor parent) {
        this.parent=parent
    }

    protected void act() {
        receive() {List<Integer> list ->
            switch (list.size()) {
                case 0:parent.send([0])
                    break
                case 1:parent.send([list[0]])
                    break
                case 2:parent.send([list[0] + list[1]])
                    break
                default:
                    def splitList1
                    def splitList2
                    (splitList1, splitList2) = split(list)
                    Actor replyActor = new ReplyActor(parent).start()
                    new Processor(replyActor).start().send(splitList1)
                    new Processor(replyActor).start().send(splitList2)
            }
        }
        stop()
    }

    private split(List<Integer> list) {
        int listSize=list.size()
        int middleIndex = listSize / 2
        def list1=list[0..<middleIndex]
        def list2=list[middleIndex..listSize-1]
        return [list1, list2]
    }
}

class ReplyActor extends DefaultThreadActor {

    Actor parent

    def ReplyActor(Actor parent) {
        this.parent = parent
    }

    void act() {
        def sum=0
        
        receive {
            sum+=it[0]
            receive {
                sum+=it[0]
            }
        }
        parent.send([sum])
        stop()
    }
}
class ArrayCalculator extends DefaultThreadActor {

    List<Integer> listToCalculate;

    AtomicInteger result=new AtomicInteger(0)

    CountDownLatch latch=new CountDownLatch(1)

    def ArrayCalculator(final List<Integer> listToCalculate) {
        this.listToCalculate = listToCalculate;
    }

    protected void act() {
        new Processor(this).start().send(listToCalculate)
        receive {
            result.set it[0]
            latch.countDown()
        }
        stop()
    }
}
