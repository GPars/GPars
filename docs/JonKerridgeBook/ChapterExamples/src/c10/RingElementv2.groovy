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

class RingElementv2 implements CSProcess {

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
        def emptyPacket = new RingPacket(source: -1, destination: -1, value: -1, full: false)
        def localBuffer = new RingPacket()
        def localBufferFull = false
        toRing.write(emptyPacket)
        while (true) {
            def index = ringAlt.select(preCon)
            switch (index) {
                case RING:
                    def ringBuffer = (RingPacket) fromRing.read()
                    //println "REv2: Element ${element} has read ${ringBuffer.toString()} from ring"
                    if (ringBuffer.destination == element) {  // packet for this node; full should be true
                        toLocal.write(ringBuffer)     // write to local
                        // now write either empty packet or a full localBuffer to ring
                        if (localBufferFull) {
                            //println "..REv2: Element ${element} writing ${localBuffer.toString()} to local"
                            toRing.write(localBuffer)
                            preCon[LOCAL] = true          // allow another packet from Sender
                            localBufferFull = false
                        }
                        else {
                            toRing.write(emptyPacket)
                            //println "--REv2: Element ${element} written emptyPacket toRing"
                        }
                    }
                    else {
                        if (ringBuffer.full) {
                            // packet for onward transmission to another element
                            //println "++REv2: Element ${element} writing ${ringBuffer.toString()} onwards"
                            toRing.write(ringBuffer)
                        }
                        else {
                            // have received an empty packet can output the localBuffer if full
                            if (localBufferFull) {
                                //println "==REv2: Element ${element} writing local ${localBuffer.toString()} to ring"
                                toRing.write(localBuffer)
                                preCon[LOCAL] = true          // allow another packet from Sender
                                localBufferFull = false
                            }
                            else {
                                toRing.write(emptyPacket)
                                //println "##REv2: Element ${element} written emptyPacket toRing"
                            }
                        }
                    }
                    break
                case LOCAL:
                    localBuffer = fromLocal.read()
                    preCon[LOCAL] = false             // stop any more Sends until buffer is emptied
                    localBufferFull = true
                    //println "@@REv2: Element ${element} read ${localBuffer.toString()} into localBuffer"
                    break
            }  // end switch
        }
    }
}
      