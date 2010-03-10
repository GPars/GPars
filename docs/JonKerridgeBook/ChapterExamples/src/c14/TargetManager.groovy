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
import org.jcsp.groovy.*

class TargetManager implements CSProcess {
    def ChannelInput targetIdFromTarget
    def ChannelInput getActiveTargets
    def ChannelOutput activatedTargets
    def ChannelOutput activatedTargetsToDC
    def ChannelInput targetsFlushed
    def ChannelOutput flushNextBucket
    def Barrier setUpBarrier

    void run() {
        setUpBarrier.sync()
        //println "TM: setup sync"
        while (true) {
            def targetList = []
            getActiveTargets.read()
            flushNextBucket.write(1)
            def targetsRunning = targetsFlushed.read()
            while (targetsRunning > 0) {
                targetList << targetIdFromTarget.read()
                targetsRunning = targetsRunning - 1
            }
            activatedTargets.write(targetList)
            activatedTargetsToDC.write(targetList)
        }
    }
}