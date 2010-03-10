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
import org.jcsp.net.*

class AgentExtraElement implements CSProcess {

    def ChannelInput fromRing
    def ChannelOutput toRing

    def void run() {
        def One2OneChannel N2A = Channel.createOne2One()
        def One2OneChannel A2N = Channel.createOne2One()

        def ChannelInput toAgentInEnd = N2A.in()
        def ChannelInput fromAgentInEnd = A2N.in()
        def ChannelOutput toAgentOutEnd = N2A.out()
        def ChannelOutput fromAgentOutEnd = A2N.out()

        println "Extra Element starting ..."
        def NetChannelLocation originalToRing = toRing.getChannelLocation()
        def emptyPacket = new RingPacket(source: -1, destination: -1, value: -1, full: false)
        def element = 0     // by default

        while (true) {
            def ringBuffer = fromRing.read()
            if (ringBuffer instanceof RingPacket) {
                toRing.write(ringBuffer)
            }
            else {
                if (ringBuffer instanceof StopAgent) {
                    def theAgent = ringBuffer
                    theAgent.connect([fromAgentOutEnd, toAgentInEnd])
                    def agentManager = new ProcessManager(theAgent)
                    agentManager.start()
                    def failedNode = fromAgentInEnd.read()
                    def targetNode = fromAgentInEnd.read()
                    def alreadyInitialised = fromAgentInEnd.read()
                    if (!alreadyInitialised) {
                        toAgentOutEnd.write(fromRing.getChannelLocation())
                    }
                    if (element == targetNode) {
                        // got to node that needs to be changed
                        toAgentOutEnd.write(true)
                        def NetChannelLocation revisedToRing = fromAgentInEnd.read()
                        toRing = NetChannelEnd.createAny2Net(revisedToRing)
                        agentManager.join()
                        theAgent.disconnect()
                        println "Node $element: stopping has redirected toRing"
                        // no need to send agent any further its got to its target
                        // ring has lost a node hence do not send an empty packet
                    }
                    else {
                        toAgentOutEnd.write(false)
                        agentManager.join()
                        theAgent.disconnect()
                        toRing.write(theAgent)
                        println "Node $element: stopping has passed agent on to next node"
                    }
                }
                else {
                    // must be instance of RestartAgent
                    def theAgent = ringBuffer
                    theAgent.connect([fromAgentOutEnd, toAgentInEnd])
                    def agentManager = new ProcessManager(theAgent)
                    agentManager.start()
                    def firstHop = fromAgentInEnd.read()
                    def resumedNode = fromAgentInEnd.read()
                    def targetNode = fromAgentInEnd.read()
                    if (firstHop) {
                        agentManager.join()
                        theAgent.disconnect()
                        toRing.write(theAgent)
                    }
                    else {
                        if (element == targetNode) {
                            toRing = NetChannelEnd.createAny2Net(originalToRing)
                            println "Node $element: restarting has redirected toRing"
                            agentManager.join()
                            theAgent.disconnect()
                            // no need to send agent any further its got to its target
                            // but the node has been reinstated hence need another packet on ring
                            toRing.write(emptyPacket)
                        }
                        else {
                            agentManager.join()
                            theAgent.disconnect()
                            toRing.write(theAgent)
                            println "Node $element: restarting has passed agent on to next node"
                        }

                    }
                }
            }
        }
    }
}