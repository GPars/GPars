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

class RingElementv1 implements CSProcess {

    def ChannelInput fromRing
    def ChannelOutput toRing
    def ChannelInput fromLocal
    def ChannelOutput toLocal
    def int element

    def void run() {
        def RING = 0
        def LOCAL = 1
        def ringAlt = new ALT([fromRing, fromLocal])
        def preCon = new boolean[2]
        preCon[RING] = true
        preCon[LOCAL] = true
        while (true) {
            def index = ringAlt.select(preCon)
            switch (index) {
                case RING:
                    def packet = (RingPacket) fromRing.read()
                    println "REv1: Element ${element} has read ${packet.toString()}"
                    if (packet.destination == element) {  // packet for this node
                        if (packet.full) {        // packet from elsewhere
                            println "--REv1: Element ${element} writing ${packet.toString()} to local"
                            toLocal.write(packet.copy())     // write to local
                            packet.destination = packet.source
                            packet.source = element
                            packet.full = false
                            println "**REv1: Element ${element} writing empty ${packet.toString()} to source"
                            toRing.write(packet)      //send modifed packet back to source
                        }
                        else {    //returned empty packet
                            println "##REv1: Element ${element} received empty ${packet.toString()}"
                            preCon[LOCAL] = true    // can accept a packet from local node
                        }
                    }
                    else { // packet for another node
                        println "..REv1: Element ${element} writing ${packet.toString()} onwards"
                        toRing.write(packet)
                    }
                    break
                case LOCAL:
                    def packet = (RingPacket) fromLocal.read()
                    println "++REv1: Element ${element} writing local ${packet.toString()} to ring"
                    toRing.write(packet)
                    preCon[LOCAL] = false     // stop local from sending any more packets
                    break
            }
        }
    }
}

    
        