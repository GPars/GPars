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

class Elementv2 implements CSProcess {

    def ChannelInput fromRing
    def ChannelOutput toRing
    def int element
    def int nodes
    def int iterations = 12

    def void run() {
        One2OneChannel S2RE = Channel.createOne2One()
        One2OneChannel RE2R = Channel.createOne2One()
        One2OneChannel R2GEC = Channel.createOne2One()

        def nodeList = [new Sender(toElement: S2RE.out(),
                element: element,
                nodes: nodes,
                iterations: iterations),
                new Receiver(fromElement: RE2R.in(),
                        outChannel: R2GEC.out(),
                        element: element),
                new RingElementv2(fromLocal: S2RE.in(),
                        toLocal: RE2R.out(),
                        fromRing: fromRing,
                        toRing: toRing,
                        element: element),
                new GConsole(toConsole: R2GEC.in(),
                        frameLabel: "Element: " + element)
        ]
        new PAR(nodeList).run()
    }
}
    
  