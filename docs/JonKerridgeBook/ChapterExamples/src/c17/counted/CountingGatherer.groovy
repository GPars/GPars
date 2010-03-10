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

package c17.counted

import org.jcsp.groovy.*
import org.jcsp.lang.*

class CountingGatherer implements CSProcess {

    def ChannelInput inChannel
    def ChannelOutput outChannel
    def ChannelOutput gatheredData
    def ChannelInput countInput

    void run() {
        def counter = 0
        def required = 0
        def gatherAlt = new ALT([countInput, inChannel])
        while (true) {
            def index = gatherAlt.priSelect()
            if (index == 0) {
                required = countInput.read()
            }
            else {
                def v = inChannel.read()
                counter = counter + 1
                outChannel.write(v)
                if (counter == required) {
                    println "Gathered value was ${v}"
                    def cv = new CountedData(counter: counter, value: v)
                    gatheredData.write(cv)
                }
            }
        }
    }
}