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

package c10

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.groovy.plugAndPlay.*

class RingElementv0 implements CSProcess {
    def ChannelInput fromRing
    def ChannelOutput toRing
    def ChannelInput fromLocal
    def ChannelOutput toLocal
    def int element

    def void run() {
        def ringAlt = new ALT([fromRing, fromLocal])
        def RING = 0
        def LOCAL = 1
        while (true) {
            def index = ringAlt.priSelect()
            switch (index) {
                case RING:
                    def packet = (RingPacket) fromRing.read()
                    println "REv0: Element ${element} has read ${packet.toString()}"
                    if (packet.destination == element) {
                        println "..REv0: Element ${element} writing packet from ring to local"
                        toLocal.write(packet)
                    }
                    else {
                        println "--REv0: Element ${element} writing packet from ring to ring"
                        toRing.write(packet)
                    }
                    break
                case LOCAL:
                    def packet = (RingPacket) fromLocal.read()
                    println "**REv0: Element ${element} writing local ${packet.toString()} to ring"
                    toRing.write(packet)
                    break
            }
        }
    }
}

    
        