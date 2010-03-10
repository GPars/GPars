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

package c17.sniff

import org.jcsp.lang.*
import org.jcsp.groovy.*
import c5.*

class Comparator implements CSProcess {

    def ChannelInput fromSystemOutput
    def ChannelInput fromSniffer

    void run() {
        def SNIFF = 0
        def COMPARE = 1
        def comparatorAlt = new ALT([fromSniffer, fromSystemOutput])
        def running = true
        while (running) {
            def index = comparatorAlt.priSelect()
            switch (index) {
                case SNIFF:
                    def value = fromSniffer.read()
                    def comparing = true
                    while (comparing) {
                        def result = (ScaledData) fromSystemOutput.read()
                        if (result.original == value) {
                            if (result.scaled >= result.original) {
                                println "\t\t\t\t\t WITHIN BOUNDS: ${result}"
                                comparing = false
                            }
                            else {
                                println "\t\t\t\t\t OUTWITH BOUNDS: ${result}"
                                running = false
                            }
                        }
                    }
                    break
                case COMPARE:
                    fromSystemOutput.read()
                    break
            }
        }
    }
}