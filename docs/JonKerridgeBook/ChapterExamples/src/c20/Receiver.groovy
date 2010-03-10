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

package c20

import org.jcsp.lang.*
import org.jcsp.groovy.*

class Receiver implements CSProcess {

    def ChannelInput fromElement
    def ChannelOutput outChannel
    def ChannelOutput clear
    def ChannelInput fromConsole

    def void run() {
        def recAlt = new ALT([fromConsole, fromElement])
        def CONSOLE = 0
        def ELEMENT = 1
        while (true) {
            def index = recAlt.priSelect()
            switch (index) {
                case CONSOLE:
                    def state = fromConsole.read()
                    outChannel.write("\n go to restart")
                    clear.write("\n")
                    while (state != "\ngo") {
                        state = fromConsole.read()
                        outChannel.write("\n go to restart")
                        clear.write("\n")
                    }
                    outChannel.write("\nresuming ...\n")
                    break
                case ELEMENT:
                    def packet = fromElement.read()
                    outChannel.write("Received: " + packet.toString() + "\n")
                    break
            }
        }
    }
}

