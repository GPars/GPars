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

package c18

import org.jcsp.lang.*
import org.jcsp.groovy.*

class Root implements CSProcess {

    def ChannelInput inChannel
    def ChannelOutput outChannel
    def int iterations
    def String initialValue

    void run() {
        def One2OneChannel N2A = Channel.createOne2One()
        def One2OneChannel A2N = Channel.createOne2One()
        def ChannelInput toAgentInEnd = N2A.in()
        def ChannelInput fromAgentInEnd = A2N.in()
        def ChannelOutput toAgentOutEnd = N2A.out()
        def ChannelOutput fromAgentOutEnd = A2N.out()

        def theAgent = new Agent(results: [initialValue])

        for (i in 1..iterations) {
            outChannel.write(theAgent)
            theAgent = inChannel.read()
            theAgent.connect([fromAgentOutEnd, toAgentInEnd])
            def agentManager = new ProcessManager(theAgent)
            agentManager.start()
            def returnedResults = fromAgentInEnd.read()
            println "Root: Iteration: $i is $returnedResults "
            returnedResults << "end of " + i
            toAgentOutEnd.write(returnedResults)
            agentManager.join()
            theAgent.disconnect()
        }
    }
}