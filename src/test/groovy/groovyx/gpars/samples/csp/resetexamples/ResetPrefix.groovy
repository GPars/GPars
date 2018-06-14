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

package groovyx.gpars.samples.csp.resetexamples

import groovyx.gpars.csp.ALT

import jcsp.lang.CSProcess
import jcsp.lang.ChannelInput
import jcsp.lang.ChannelOutput

class ResetPrefix implements CSProcess {

    def prefixValue = 0
    ChannelOutput outChannel
    ChannelInput inChannel
    ChannelInput resetChannel

    void run() {
        def alt = new ALT([resetChannel, inChannel])
        outChannel.write(prefixValue)
        while (true) {
            if (alt.priSelect() == 0) {    // resetChannel input
                def resetValue = resetChannel.read()
                inChannel.read()     // read the inChannel and ignore it
                outChannel.write(resetValue)
            }
            else {    //inChannel input only
                def inputValue = inChannel.read()     // and send it on through the system
                outChannel.write(inputValue)
            }
        }
    }
}
