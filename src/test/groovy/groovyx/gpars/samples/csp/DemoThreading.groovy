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
import groovyx.gpars.csp.plugAndPlay.GIdentity

import jcsp.lang.CSProcess
import jcsp.lang.Channel
import jcsp.lang.ChannelInput
import jcsp.lang.ChannelOutput
import jcsp.lang.One2OneChannel

class Threading implements CSProcess {
    final int numberOfCopiers = 1500

    void run() {

        One2OneChannel currentChannel = Channel.createOne2One()

        def testList = [new NumberGenerator(outChannel: currentChannel.out())]
        numberOfCopiers.times {
            One2OneChannel newChannel = Channel.createOne2One()
            testList << new GIdentity(inChannel: currentChannel.in(), outChannel: newChannel.out())
            currentChannel = newChannel
        }
        testList << new NumberConsumer(inChannel: currentChannel.in())
        new PAR(testList).run()
    }
}

def testList = [new Threading()]
final def par = new PAR(testList)
par.run()

class NumberGenerator implements CSProcess {

    ChannelOutput outChannel

    void run() {
        (2..1000000).each {
            outChannel.write(it)
        }
    }
}

class NumberConsumer implements CSProcess {

    ChannelInput inChannel

    void run() {
        while (true) {
            println "Read: " + inChannel.read()
        }
    }
}
