// GPars (formerly GParallelizer)
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

package c14

import org.jcsp.lang.*
import java.awt.Point
import org.jcsp.groovy.*

class TargetProcess implements CSProcess {
    def ChannelOutput targetRunning
    def ChannelOutput stateToDC
    def ChannelInput mousePoint
    def Barrier setUpBarrier
    def Barrier initBarrier
    def Barrier goBarrier
    def AltingBarrier timeAndHitBarrier
    def buckets
    def int targetId
    def int x
    def int y
    def delay = 2000

    def boolean within(Point p, int x, int y) {
        def maxX = x + 100
        def maxY = y + 100
        if (p.x < x) return false
        if (p.y < y) return false
        if (p.x > maxX) return false
        if (p.y > maxY) return false
        return true
    }

    void run() {
        def rng = new Random()
        def timer = new CSTimer()
        def int range = buckets.size() / 2
        def bucketId = rng.nextInt(range)
        def POINT = 1
        def TIMER = 0
        def BARRIER = 0

        def TIMED_OUT = 0
        def HIT = 1

        def preTimeOutAlt = new ALT([timer, mousePoint])
        def postTimeOutAlt = new ALT([timeAndHitBarrier, mousePoint])

        timeAndHitBarrier.resign()
        setUpBarrier.sync()
        //println "TP: target $targetId setup sync and falling into bucket $bucketId"
        buckets[bucketId].fallInto()
        // starting point once the process has been flushed from a bucket
        while (true) {
            timeAndHitBarrier.enroll()
            goBarrier.enroll()
            targetRunning.write(targetId)
            initBarrier.sync()    //ensures all targets have initialised
            //println "TP: target $targetId init sync"
            def running = true
            def resultList = [targetId]
            goBarrier.resign()
            //println "TP: target $targetId completed goBarrier resign"
            timer.setAlarm(timer.read() + delay + rng.nextInt(delay))
            while (running) {
                switch (preTimeOutAlt.priSelect()) {
                    case TIMER:
                        running = false
                        resultList << TIMED_OUT
                        stateToDC.write(resultList)
                        break
                    case POINT:
                        def point = mousePoint.read()
                        if (within(point, x, y)) {
                            running = false
                            resultList << HIT
                            stateToDC.write(resultList)
                        }
                        else {

                        }
                        break
                }
            }
            def awaiting = true
            while (awaiting) {
                switch (postTimeOutAlt.priSelect()) {
                    case BARRIER:
                        awaiting = false
                        timeAndHitBarrier.resign()
                        //println "TP: target $targetId has timeout or hit barriered"
                        break
                    case POINT:
                        mousePoint.read()
                        break
                }
            }
            bucketId = (bucketId + 2 + rng.nextInt(8)) % buckets.size()
            //println "TP: target $targetId falling into bucket $bucketId"
            buckets[bucketId].fallInto()
        }
    }
}
      
      
      
      
      
      
