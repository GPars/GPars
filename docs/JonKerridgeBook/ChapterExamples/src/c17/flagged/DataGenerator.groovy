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

package c17.flagged

import org.jcsp.groovy.*
import org.jcsp.lang.*

class DataGenerator implements CSProcess {

    def ChannelOutput outChannel
    def interval = 500

    void run() {
        //println "Generator Started"
        def timer = new CSTimer()
        def i = 0
        while (true) {
            def v = new SystemData(a: i, b: i + 1)
            outChannel.write(v)
            i = i + 2
            timer.sleep(interval)
        }
    }
}