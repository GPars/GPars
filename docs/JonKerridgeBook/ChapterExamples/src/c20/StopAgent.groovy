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

package c20;

import org.jcsp.net.mobile.*
import org.jcsp.net.*
import org.jcsp.lang.*
import org.jcsp.groovy.*

class StopAgent implements MobileAgent {

    def ChannelOutput toLocal
    def ChannelInput fromLocal
    def int homeNode
    def int previousNode
    def boolean initialised
    def NetChannelLocation nextNodeInputEnd

    def connect(List c) {
        this.toLocal = c[0]
        this.fromLocal = c[1]

    }

    def disconnect() {
        this.toLocal = null
        this.fromLocal = null
    }

    void run() {
        println "SA: running $homeNode, $previousNode, $initialised"
        toLocal.write(homeNode)    // tells node not to send to this node
        toLocal.write(previousNode) // where we want to get to
        toLocal.write(initialised)
        if (!initialised) {
            nextNodeInputEnd = fromLocal.read()
            initialised = true
            println "SA: initialised"
        }
        def gotThere = fromLocal.read()
        if (gotThere) {
            toLocal.write(nextNodeInputEnd)
            println "SA: got to $previousNode"
        }
    }

}