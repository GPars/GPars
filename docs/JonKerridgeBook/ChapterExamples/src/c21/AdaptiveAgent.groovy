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

package c21

import org.jcsp.net.*
import org.jcsp.lang.*
import org.jcsp.groovy.*

class AdaptiveAgent implements MobileAgent, Serializable {

    def ChannelInput fromInitialNode
    def ChannelInput fromVisitedNode
    def ChannelOutput toVisitedNode
    def ChannelOutput toReturnedNode

    def initial = true
    def visiting = false
    def returned = false

    def availableNodes = []
    def requiredProcess = null
    def returnLocation
    def processDefinition = null
    def homeNode

    def connect(List c) {
        if (initial) {
            fromInitialNode = c[0]
            returnLocation = c[1]
            homeNode = c[2]
        }
        if (visiting) {
            fromVisitedNode = c[0]
            toVisitedNode = c[1]
        }
        if (returned) {
            toReturnedNode = c[0]
        }
    }

    def disconnect() {
        fromInitialNode = null
        fromVisitedNode = null
        toVisitedNode = null
        toReturnedNode = null
    }

    void run() {
        if (returned) {
            toReturnedNode.write([processDefinition, requiredProcess])
            //println "AA: returned agent has written data to home node"
        }

        if (visiting) {
            toVisitedNode.write(requiredProcess)
            //println "AA: visitor wants $requiredProcess"
            processDefinition = fromVisitedNode.read()
            //println "AA: visitor received $processDefinition"
            if (processDefinition != null) {
                toVisitedNode.write(homeNode)
                visiting = false
                returned = true
                def nextNodeLocation = returnLocation
                def nextNodeChannel = NetChannelEnd.createOne2Net(nextNodeLocation)
                //println "AA: visitor being sent home"
                disconnect()
                nextNodeChannel.write(this)  // THIS has become NOT serializable!!
                //println "AA: visitor has returned home"
            }
            else {
                disconnect()
                //determine next node to visit and go there
                // assumes that the process is available somewhere!
                def nextNodeLocation = availableNodes.pop()
                def nextNodeChannel = NetChannelEnd.createOne2Net(nextNodeLocation)
                //println "AA: visitor continuing journey"
                nextNodeChannel.write(this)
                //println "AA: visitor has continued journey"
            }
        }

        if (initial) {
            def awaitingTypeName = true
            while (awaitingTypeName) {
                def d = fromInitialNode.read()
                if (d instanceof List) {
                    for (i in 0..<d.size) { availableNodes << d[i] }
                }
                if (d instanceof String) {
                    requiredProcess = d
                    awaitingTypeName = false
                    initial = false
                    visiting = true
                    disconnect()
                    //determine next node to visit and go there
                    def nextNodeLocation = availableNodes.pop()
                    def nextNodeChannel = NetChannelEnd.createOne2Net(nextNodeLocation)
                    //println "AA: initial going visiting"
                    nextNodeChannel.write(this)
                    //println "AA: initial has been sent to another node"
                }
            }
        }
    }

}