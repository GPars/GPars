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

package c20

import org.jcsp.lang.*
import org.jcsp.groovy.*

class Sender implements CSProcess {

    def ChannelOutput toElement
    def int element
    def int nodes
    def int iterations

    def void run() {
        def timer = new CSTimer()
        def start = element
        for (i in 1..iterations) {
            def dest = (start % (nodes)) + 1
            if (dest != element) {
                def packet = new RingPacket(source: element, destination: dest, value: (element * 10000) + start, full: true)
                toElement.write(packet)
                timer.sleep(500)
            }
            start = start + 1
        }
        println "Sender $element has finished"
    }
}

    
