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

import jcsp.lang.CSProcess
import jcsp.lang.Channel
import jcsp.lang.ChannelInput
import jcsp.lang.ChannelOutput
import jcsp.lang.One2OneChannel

final int requestedPrimeNumberCount = 100000

One2OneChannel initialChannel = Channel.createOne2One()

final def par = new PAR([
        new Generator(outChannel: initialChannel.out()),
])
Thread.start {
    par.run()
}

def filter(inChannel, int prime) {
    def outChannel = Channel.createOne2One()
    def par = new PAR([new Filter(inChannel: inChannel, outChannel: outChannel.out(), prime: prime)])
    Thread.start {
        par.run()
    }
    return outChannel.in()
}

def currentOutput = initialChannel.in()
requestedPrimeNumberCount.times {
    int prime = currentOutput.read()
    println "Found: $prime"
    currentOutput = filter(currentOutput, prime)
}

class Generator implements CSProcess {

    ChannelOutput outChannel

    void run() {
        (2..1000000).each {
            outChannel.write(it)
        }
    }
}

class Filter implements CSProcess {

    ChannelInput inChannel
    ChannelOutput outChannel
    int prime


    def Filter() {

    }

    void run() {
        while (true) {
            def number = inChannel.read()
            if (number % prime != 0) {
                outChannel.write(number)
            }
        }
    }
}
