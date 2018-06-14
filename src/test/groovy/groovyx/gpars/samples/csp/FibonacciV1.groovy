// GPars — Groovy Parallel Systems
//
// Copyright © 2008–2010, 2018  The original author or authors
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

package groovyx.gpars.samples.csp

import groovyx.gpars.csp.PAR
import groovyx.gpars.csp.plugAndPlay.GPrefix
import groovyx.gpars.csp.plugAndPlay.GPCopy
import groovyx.gpars.csp.plugAndPlay.GStatePairs
import groovyx.gpars.csp.plugAndPlay.GPrint

import jcsp.lang.CSProcess
import jcsp.lang.Channel
import jcsp.lang.ChannelOutput
import jcsp.lang.One2OneChannel

class FibonacciV1Process implements CSProcess {

    ChannelOutput outChannel

    void run() {

        One2OneChannel a = Channel.createOne2One()
        One2OneChannel b = Channel.createOne2One()
        One2OneChannel c = Channel.createOne2One()
        One2OneChannel d = Channel.createOne2One()

        def testList = [
                new GPrefix(prefixValue: 0, inChannel: d.in(), outChannel: a.out()),
                new GPrefix(prefixValue: 1, inChannel: c.in(), outChannel: d.out()),
                new GPCopy(inChannel: a.in(), outChannel0: b.out(), outChannel1: outChannel),
                new GStatePairs(inChannel: b.in(), outChannel: c.out()),
        ]
        new PAR(testList).run()
    }
}

One2OneChannel N2P = Channel.createOne2One()

def testList = [
        new FibonacciV1Process(outChannel: N2P.out()),
        new GPrint(inChannel: N2P.in(), heading: "Fibonacci Numbers")
]

final def par = new PAR(testList)
par.run()
