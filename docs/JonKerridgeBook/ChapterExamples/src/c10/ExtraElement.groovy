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

class ExtraElement implements CSProcess {
    def ChannelInput fromRing
    def ChannelOutput toRing

    def void run() {
        def packet = new RingPacket(source: -1, destination: -1, value: -1, full: false)
        while (true) {
            toRing.write(packet)
            println "Extra Element 0 has written " + packet.toString()
            packet = (RingPacket) fromRing.read()
            println "Extra Element 0 has read " + packet.toString()
        }
    }
}
 
      
