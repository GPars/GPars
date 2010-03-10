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

import org.jcsp.groovy.*
import org.jcsp.lang.*
import phw.util.*

def int nodes = Ask.Int("Number of Nodes ? ", 1, 9)
def int iterations = Ask.Int("Number of Iterations ? ", 1, 9)
def String initialValue = Ask.string("Initial List Value ? ")

def One2OneChannel[] ring = Channel.createOne2One(nodes + 1)

def processNodes = (1..<nodes).collect {i ->
    new ProcessNode(inChannel: ring[i].in(),
            outChannel: ring[i + 1].out(),
            nodeId: i)
}

processNodes << new ProcessNode(inChannel: ring[nodes].in(),
        outChannel: ring[0].out(),
        nodeId: nodes)

def rootNode = new Root(inChannel: ring[0].in(),
        outChannel: ring[1].out(),
        iterations: iterations,
        initialValue: initialValue)

def network = processNodes << rootNode

new PAR(network).run()