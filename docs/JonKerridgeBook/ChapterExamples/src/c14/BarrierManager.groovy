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


class BarrierManager implements CSProcess {
    def AltingBarrier timeAndHitBarrier
    def AltingBarrier finalBarrier
    def Barrier goBarrier
    def Barrier setUpBarrier

    void run() {
        def timeHitAlt = new ALT([timeAndHitBarrier])
        def finalAlt = new ALT([finalBarrier])
        setUpBarrier.sync()
        //println "    BM: synced on setup awaiting go"
        while (true) {
            goBarrier.sync()
            //println "    BM: synced on go "
            def t = timeHitAlt.select()
            //println "    BM: barriered on timeout or hit "
            def f = finalAlt.select()
            //println "    BM: has barriered on final"
        }
    }

}