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

package c3

import org.jcsp.groovy.plugAndPlay.*

import org.jcsp.lang.*
import org.jcsp.groovy.*

One2OneChannel[] connect = Channel.createOne2One(5)
One2OneChannel[] outChans = Channel.createOne2One(3)

def printList = new ChannelInputList(outChans)

def titles = ["n", "int", "sqr"]

def testList = [new GNumbers(outChannel: connect[0].out()),
        new GPCopy(inChannel: connect[0].in(),
                outChannel0: connect[1].out(),
                outChannel1: outChans[0].out()),
        new GIntegrate(inChannel: connect[1].in(),
                outChannel: connect[2].out()),
        new GPCopy(inChannel: connect[2].in(),
                outChannel0: connect[3].out(),
                outChannel1: outChans[1].out()),
        new GPairs(inChannel: connect[3].in(),
                outChannel: connect[4].out()),
        new GPrefix(prefixValue: 0,
                inChannel: connect[4].in(),
                outChannel: outChans[2].out()),
        new GParPrint(inChannels: printList,
                headings: titles)
]
new PAR(testList).run()