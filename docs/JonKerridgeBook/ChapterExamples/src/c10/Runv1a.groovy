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

package c10;


import org.jcsp.lang.*
import org.jcsp.groovy.*
import phw.util.*

println "Starting v1 ..."
def nodes = Ask.Int("Number of Nodes ? - ", 3, 10)

One2OneChannel[] ring = Channel.createOne2One(nodes + 1)

def extra = new ExtraElementv1(fromRing: ring[0].in(),
        toRing: ring[1].out())

def elementList = (1..nodes).collect {i ->
    def toR = (i + 1) % (nodes + 1)
    println "Creating Element: ${i} from  ${i} to ${toR}"
    return new Elementv1(fromRing: ring[i].in(),
            toRing: ring[toR].out(),
            element: i,
            nodes: nodes)
}

new PAR(elementList + extra).run()