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

package c17.sniff

import org.jcsp.lang.*
import org.jcsp.groovy.*

class Sniffer implements CSProcess {

    def ChannelInput fromSystemCopy
    def ChannelOutput toComparator
    def sampleInterval = 10000

    void run() {
        def TIME = 0
        def INPUT = 1
        def timer = new CSTimer()
        def snifferAlt = new ALT([timer, fromSystemCopy])
        def timeout = timer.read() + sampleInterval
        timer.setAlarm(timeout)
        while (true) {
            def index = snifferAlt.select()
            switch (index) {
                case TIME:
                    toComparator.write(fromSystemCopy.read())
                    timeout = timer.read() + sampleInterval
                    timer.setAlarm(timeout)
                    break
                case INPUT:
                    fromSystemCopy.read()
                    break
            }
        }
    }
}