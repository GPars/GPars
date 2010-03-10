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
import org.jcsp.net.*
import org.jcsp.net.tcpip.*
import org.jcsp.net.cns.*
import org.jcsp.groovy.*
import phw.util.*

Node.getInstance().init(new TCPIPNodeFactory())

def int iterations = Ask.Int("Number of Iterations ? ", 1, 9)
def String initialValue = Ask.string("Initial List Value ? ")

def fromRingName = "ring0"
def toRingName = "ring1"

def fromRing = CNS.createNet2One(fromRingName)
def toRing = CNS.createOne2Net(toRingName)

println " Root: connection from $fromRingName to $toRingName "

def rootNode = new Root(inChannel: fromRing,
        outChannel: toRing,
        iterations: iterations,
        initialValue: initialValue)

new PAR([rootNode]).run()
