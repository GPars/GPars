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

package c9

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.groovy.plugAndPlay.*

def hoppers = 3

One2OneChannel[] h2m = Channel.createOne2One(hoppers)
One2OneChannel[] m2h = Channel.createOne2One(hoppers)

One2OneChannel[] h2c = Channel.createOne2One(hoppers)
One2OneChannel[] c2h = Channel.createOne2One(hoppers)
One2OneChannel[] clearHC = Channel.createOne2One(hoppers)

One2OneChannel b2m = Channel.createOne2One()
One2OneChannel m2b = Channel.createOne2One()

One2OneChannel b2c = Channel.createOne2One()
One2OneChannel c2b = Channel.createOne2One()
One2OneChannel clearBC = Channel.createOne2One()

def h2mList = new ChannelInputList(h2m)
def m2hList = new ChannelOutputList(m2h)

//for ( i in 0 ..< hoppers) {
//	hb2mList.append( h2m[i].in())
//	m2hbList.append( m2h[i].out())
//}


def hopperList = (0..<hoppers).collect {i ->
    new Hopper(fromConsole: c2h[i].in(),
            toConsole: h2c[i].out(),
            clearConsole: clearHC[i].out(),
            toManager: h2m[i].out(),
            fromManager: m2h[i].in()
    )
}

def blender = new Blender(fromConsole: c2b.in(),
        toConsole: b2c.out(),
        clearConsole: clearBC.out(),
        toManager: b2m.out(),
        fromManager: m2b.in()
)

def manager = new Manager1Only(inputs: h2mList,
        outputs: m2hList,
        fromBlender: b2m.in(),
        toBlender: m2b.out())

def hopperConsoles = (0..<hoppers).collect {i ->
    new GConsole(toConsole: h2c[i].in(),
            fromConsole: c2h[i].out(),
            clearInputArea: clearHC[i].in(),
            frameLabel: "Hopper-" + i)
}

def blenderConsole = new GConsole(toConsole: b2c.in(),
        fromConsole: c2b.out(),
        clearInputArea: clearBC.in(),
        frameLabel: "Blender")

def procList = hopperList + blender + manager + hopperConsoles + blenderConsole

new PAR(procList).run()